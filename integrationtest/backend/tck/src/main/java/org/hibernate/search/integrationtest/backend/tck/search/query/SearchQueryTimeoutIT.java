/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assume.assumeTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.query.SearchScroll;
import org.hibernate.search.engine.search.query.SearchScrollResult;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.TckConfiguration;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.SearchTimeoutException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

public class SearchQueryTimeoutIT {

	private static final String EMPTY_FIELD_NAME = "emptyFieldName";
	private static final int NON_MATCHING_INTEGER = 739;

	// Increasing this will increase query execution time.
	private static final int DOCUMENT_COUNT = 100;

	@ClassRule
	public static SearchSetupHelper setupHelper = new SearchSetupHelper();

	private static final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@BeforeClass
	public static void setup() {
		setupHelper.start().withIndex( index ).setup();

		index.bulkIndexer()
				.add( DOCUMENT_COUNT, i -> StubMapperUtils.documentProvider( String.valueOf( i ), document -> {} ) )
				.join();
	}

	@Test
	public void fetch_failAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		assertThatThrownBy( () -> query.fetch( 20 ) )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( "Operation exceeded the timeout of 0s, 0ms and 1ns" );
	}

	@Test
	public void fetchTotalHitCount_failAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		assertThatThrownBy( () -> query.fetchTotalHitCount() )
				.isInstanceOf( SearchTimeoutException.class )
				.hasMessageContaining( "Operation exceeded the timeout of 0s, 0ms and 1ns" );
	}

	@Test
	public void scroll_failAfter_slowQuery_smallTimeout() {
		SearchQuery<DocumentReference> query = startSlowQuery()
				.failAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 5 ) ) {
			assertThatThrownBy( () -> scroll.next() )
					.isInstanceOf( SearchTimeoutException.class )
					.hasMessageContaining( "Operation exceeded the timeout of 0s, 0ms and 1ns" );
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
				.hasMessageContaining(
						"Unable to provide the exact total hit count: only a lower-bound approximation is available.",
						"This is generally the result of setting query options such as a timeout or the total hit count threshold",
						"unset these options, or retrieve the lower-bound hit count approximation"
				);
	}

	@Test
	public void scroll_truncateAfter_slowQuery_smallTimeout() {
		assumeTrue(
				"The backend doesn't support truncateAfter() on scrolls",
				TckConfiguration.get().getBackendFeatures().supportsTruncateAfterForScroll()
		);

		SearchQuery<DocumentReference> query = startSlowQuery()
				.truncateAfter( 1, TimeUnit.NANOSECONDS )
				.toQuery();

		try ( SearchScroll<DocumentReference> scroll = query.scroll( 5 ) ) {
			SearchScrollResult<DocumentReference> result = scroll.next();
			assertThat( result.took() ).isNotNull(); // May be 0 due to low resolution
			assertThat( result.timedOut() ).isTrue();

			assertThat( result.hits() ).hasSizeLessThan( 5 );
			assertThat( result.total().isHitCountLowerBound() ).isTrue();
			assertThat( result.total().hitCountLowerBound() ).isLessThanOrEqualTo( 5 );
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
			assertThat( result.total().isHitCountExact() ).isTrue();
			assertThat( result.total().hitCount() ).isEqualTo( 0 );
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
			assertThat( result.total().isHitCountExact() ).isTrue();
			assertThat( result.total().hitCount() ).isEqualTo( 0 );
		}
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> startSlowQuery() {
		return index.createScope().query()
				.where( f -> TckConfiguration.get().getBackendHelper().createSlowPredicate( f ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> startFastQuery() {
		return index.createScope().query()
				.where( f -> f.match().field( EMPTY_FIELD_NAME ).matching( NON_MATCHING_INTEGER ) );
	}

	private static class IndexBinding {
		private final List<IndexFieldReference<String>> fields = new ArrayList<>();

		IndexBinding(IndexSchemaElement root) {
			root.field( EMPTY_FIELD_NAME, f -> f.asInteger() ).toReference();
		}
	}
}
