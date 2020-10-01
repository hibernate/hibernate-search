/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SearchQueryTimeoutIT {

	// Increasing this will increase query execution time on ES proportionally, with a factor of ~1.1
	// Increasing this will increase the risk of Elasticsearch failing to index, due to documents getting bigger
	// and taking up more memory.
	private static final int FIELD_COUNT = 400;
	private static final List<String> FIELD_NAMES = Collections.unmodifiableList(
			IntStream.range( 0 , FIELD_COUNT )
					.mapToObj( i -> "field_" + i )
					.collect( Collectors.toList() )
	);
	private static final String EMPTY_FIELD_NAME = "emptyFieldName";

	private static final String MATCHING_WORD = "111word";

	private static final int NON_MATCHING_INTEGER = 739;

	// Increasing this will increase query execution time on ES proportionally, with a factor of ~1.0
	// Increasing this will not increase the risk of Elasticsearch failing to index,
	// because BulkIndexer takes care of limiting the amount of documents indexed in parallel.
	private static final int DOCUMENT_COUNT = 10_000;

	// Increasing this will increase query execution time on ES proportionally, with a factor of ~0.9
	// Increasing this will increase the risk of Elasticsearch failing to index, due to documents getting bigger
	// and taking up more memory.
	private static final int WORDS_PER_STRING = 5;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( DOCUMENT_COUNT, i -> StubMapperUtils.documentProvider( String.valueOf( i ), document -> {
					for ( int fieldOrdinal = 0; fieldOrdinal < index.binding().fields.size(); fieldOrdinal++ ) {
						IndexFieldReference<String> field = index.binding().fields.get( fieldOrdinal );
						document.addValue( field, generateText( i, fieldOrdinal ) );
					}
				} ) )
				.join();
	}

	@Test
	public void fetch_failAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		assertThatThrownBy( () -> query.fetch( 20 ) )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( " exceeded the timeout of 0s, 0ms and 1ns: " );
	}

	@Test
	public void fetchTotalHitCount_failAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		assertThatThrownBy( () -> query.fetchTotalHitCount() )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( " exceeded the timeout of 0s, 0ms and 1ns: " );
	}

	@Test
	public void scroll_failAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 5 ) ) {
			assertThatThrownBy( () -> scroll.next() )
					.isInstanceOf( SearchTimeoutException.class )
					.hasMessageContaining( " exceeded the timeout of 0s, 0ms and 1ns: " );
		}
	}

	@Test
	public void fetch_truncateAfter_slowQuery_smallTimeout() {
		SearchResult<DocumentReference> result = startSlowQuery()
				.truncateAfter( 1, TimeUnit.NANOSECONDS )
				.fetch( 20 );

		assertThat( result.took() ).isNotNull(); // May be 0 due to low resolution
		assertThat( result.timedOut() ).isTrue();

		// we cannot have an exact hit count in case of limitFetching-timeout event
		assertThat( result.total().hitCountLowerBound() ).isLessThan( DOCUMENT_COUNT );
		assertThatThrownBy( () -> result.total().hitCount() )
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "Trying to get the exact total hit count, but it is a lower bound" );
	}

	@Test
	public void scroll_truncateAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.truncateAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 5 ) ) {
			SearchScrollResult<DocumentReference> result = scroll.next();
			assertThat( result.took() ).isNotNull(); // May be 0 due to low resolution
			assertThat( result.timedOut() ).isTrue();

			assertThat( result.hits() ).hasSizeLessThan( 5 );
		}
	}

	@Test
	public void fetch_failAfter_fastQuery_largeTimeout() {
		SearchResult<DocumentReference> result = startFastQuery()
				.failAfter( 1, TimeUnit.DAYS )
				.fetch( 20 );

		assertThat( result.hits() ).hasSize( 0 );

		assertThat( result.took() ).isLessThan( Duration.ofDays( 1L ) );
		assertThat( result.timedOut() ).isFalse();
	}

	@Test
	public void fetchTotalHitCount_failAfter_fastQuery_largeTimeout() {
		SearchQuery<DocumentReference> query = startFastQuery()
				.failAfter( 1, TimeUnit.DAYS )
				.toQuery();

		assertThat( query.fetchTotalHitCount() ).isEqualTo( 0 );
	}

	@Test
	public void scroll_failAfter_fastQuery_largeTimeout() {
		SearchQuery<DocumentReference> query = startFastQuery()
				.failAfter( 1, TimeUnit.DAYS )
				.toQuery();

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 5 ) ) {
			SearchScrollResult<DocumentReference> result = scroll.next();
			assertThat( result.took() ).isLessThan( Duration.ofDays( 1L ) );
			assertThat( result.timedOut() ).isFalse();

			assertThat( result.hits() ).hasSize( 0 );
		}
	}

	@Test
	public void fetch_truncateAfter_fastQuery_largeTimeout() {
		SearchResult<DocumentReference> result = startFastQuery()
				.truncateAfter( 1, TimeUnit.DAYS )
				.fetch( 20 );

		assertThat( result.took() ).isLessThan( Duration.ofDays( 1L ) );
		assertThat( result.timedOut() ).isFalse();
	}

	@Test
	public void scroll_truncateAfter_fastQuery_largeTimeout() {
		SearchQuery<DocumentReference> query = startFastQuery()
				.truncateAfter( 1, TimeUnit.DAYS )
				.toQuery();

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 5 ) ) {
			SearchScrollResult<DocumentReference> result = scroll.next();
			assertThat( result.took() ).isLessThan( Duration.ofDays( 1L ) );
			assertThat( result.timedOut() ).isFalse();

			assertThat( result.hits() ).hasSize( 0 );
		}
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> startSlowQuery() {
		return index.createScope().query()
				.where( f -> f.bool( b -> {
					for ( int fieldOrdinal = 0; fieldOrdinal < FIELD_NAMES.size(); fieldOrdinal++ ) {
						String fieldName = FIELD_NAMES.get( fieldOrdinal );
						// Suffix query => this should be slow.
						SearchPredicate predicate = f.wildcard().field( fieldName )
								.matching( "*" + MATCHING_WORD )
								.boost( fieldOrdinal + 1 )
								.toPredicate();
						// should + different boost per predicate
						// => all predicates must be executed to compute the score
						// => even slower
						b.should( predicate );
					}
				} ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> startFastQuery() {
		return index.createScope().query()
				.where( f -> f.match().field( EMPTY_FIELD_NAME ).matching( NON_MATCHING_INTEGER ) );
	}

	private static String generateText(int documentId, int fieldOrdinal) {
		StringBuilder builder = new StringBuilder();
		int base = documentId * WORDS_PER_STRING * FIELD_NAMES.size() + fieldOrdinal * WORDS_PER_STRING;
		// Generate lots of words, as unique as possible
		for ( int wordIndex = 0; wordIndex < WORDS_PER_STRING; wordIndex++ ) {
			builder.append( " " ).append( base + wordIndex );
		}
		// And then add a matching word
		builder.append( base + "_" + MATCHING_WORD );
		return builder.toString();
	}

	private static class IndexBinding {
		private final List<IndexFieldReference<String>> fields = new ArrayList<>();

		IndexBinding(IndexSchemaElement root) {
			for ( String fieldName : FIELD_NAMES ) {
				fields.add(
						root.field(
								fieldName,
								f -> f.asString().analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name )
						)
								.toReference()
				);
			}
			root.field( EMPTY_FIELD_NAME, f -> f.asInteger() ).toReference();
		}
	}
}
