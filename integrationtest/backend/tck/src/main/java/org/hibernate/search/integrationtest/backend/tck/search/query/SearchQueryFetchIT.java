/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalize;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchHitsAssert.assertThatHits;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThatResult;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchQueryFetchIT {

	private static final int DOCUMENT_COUNT = 200;

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@Before
	public void setup() {
		setupHelper.start().withIndex( index ).setup();

		initData();
	}

	@Test
	public void fetchAll() {
		assertThatResult( matchAllQuerySortByField().fetchAll() )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatResult( matchFirstHalfQuery().fetchAll() )
				.hasTotalHitCount( DOCUMENT_COUNT / 2 )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT / 2; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );
	}

	@Test
	public void fetch_limit() {
		assertThatResult( matchAllQuerySortByField().fetch( null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatResult( matchAllQuerySortByField().fetch( 1 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ) );

		assertThatResult( matchAllQuerySortByField().fetch( 2 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ), docId( 1 ) );
	}

	@Test
	public void fetch_offset_limit() {
		assertThatResult( matchAllQuerySortByField().fetch( 1, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatResult( matchAllQuerySortByField().fetch( 1, 1 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 1 ) );

		assertThatResult( matchAllQuerySortByField().fetch( null, 2 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ), docId( 1 ) );

		assertThatResult( matchAllQuerySortByField().fetch( null, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		// Fetch beyond the total hit count
		assertThatResult( matchAllQuerySortByField().fetch( DOCUMENT_COUNT + 1, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasNoHits();
	}

	/**
	 * Same as the test above, but with the default, score sort.
	 * This is important in the Lucene implementation in particular,
	 * where the TopDocsCollector implementation will be different.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4019")
	public void fetch_offset_limit_defaultSort() {
		assertThatResult( matchAllQuerySortByDefault().fetch( 1, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsAnyOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatResult( matchAllQuerySortByDefault().fetch( 1, 1 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsAnyOrder( index.typeName(), docId( 1 ) );

		assertThatResult( matchAllQuerySortByDefault().fetch( null, 2 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsAnyOrder( index.typeName(), docId( 0 ), docId( 1 ) );

		assertThatResult( matchAllQuerySortByDefault().fetch( null, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsAnyOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		// Fetch beyond the total hit count
		assertThatResult( matchAllQuerySortByDefault().fetch( DOCUMENT_COUNT + 1, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasNoHits();
	}

	@Test
	public void fetchAllHits() {
		assertThatHits( matchAllQuerySortByField().fetchAllHits() )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatHits( matchFirstHalfQuery().fetchAllHits() )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT / 2; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );
	}

	@Test
	public void fetchHits_limit() {
		assertThatHits( matchAllQuerySortByField().fetchHits( null ) )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatHits( matchAllQuerySortByField().fetchHits( 1 ) )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ) );

		assertThatHits( matchAllQuerySortByField().fetchHits( 2 ) )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ), docId( 1 ) );
	}

	@Test
	public void fetchHits_offset_limit_fieldSort() {
		assertThatHits( matchAllQuerySortByField().fetchHits( 1, null ) )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatHits( matchAllQuerySortByField().fetchHits( 1, 1 ) )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 1 ) );

		assertThatHits( matchAllQuerySortByField().fetchHits( null, 2 ) )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ), docId( 1 ) );

		assertThatHits( matchAllQuerySortByField().fetchHits( null, null ) )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		// Fetch beyond the total hit count
		assertThatHits( matchAllQuerySortByField().fetchHits( DOCUMENT_COUNT + 1, null ) )
				.isEmpty();
	}

	/**
	 * Same as the test above, but with the default, score sort.
	 * This is important in the Lucene implementation in particular,
	 * where the TopDocsCollector implementation will be different.
	 */
	@Test
	@TestForIssue(jiraKey = "HSEARCH-4019")
	public void fetchHits_offset_limit_defaultSort() {
		assertThatHits( matchAllQuerySortByDefault().fetchHits( 1, null ) )
				.hasDocRefHitsAnyOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatHits( matchAllQuerySortByDefault().fetchHits( 1, 1 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), docId( 1 ) );

		assertThatHits( matchAllQuerySortByDefault().fetchHits( null, 2 ) )
				.hasDocRefHitsAnyOrder( index.typeName(), docId( 0 ), docId( 1 ) );

		assertThatHits( matchAllQuerySortByDefault().fetchHits( null, null ) )
				.hasDocRefHitsAnyOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		// Fetch beyond the total hit count
		assertThatHits( matchAllQuerySortByDefault().fetchHits( DOCUMENT_COUNT + 1, null ) )
				.isEmpty();
	}

	@Test
	public void fetchTotalHitCount() {
		Assertions.assertThat( matchAllQuerySortByField().fetchTotalHitCount() ).isEqualTo( DOCUMENT_COUNT );

		Assertions.assertThat( matchFirstHalfQuery().fetchTotalHitCount() ).isEqualTo( DOCUMENT_COUNT / 2 );
	}

	@Test
	public void fetchSingleHit() {
		Optional<DocumentReference> result = matchOneQuery( 4 ).fetchSingleHit();
		Assertions.assertThat( result ).isNotEmpty();
		Assertions.assertThat( normalize( result.get() ) )
				.isEqualTo( normalize( reference( index.typeName(), docId( 4 ) ) ) );

		result = matchNoneQuery().fetchSingleHit();
		Assertions.assertThat( result ).isEmpty();

		Assertions.assertThatThrownBy( () -> {
			matchAllQuerySortByField().fetchSingleHit();
		} )
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void fetch_limitAndOffset_reuseQuery() {
		SearchQuery<DocumentReference> query = matchAllQuerySortByField().toQuery();
		assertThatResult( query.fetch( 1, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatResult( query.fetch( 1, 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 1 ) );

		assertThatResult( query.fetch( null, 2 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( index.typeName(), docId( 0 ), docId( 1 ) );

		assertThatResult( query.fetch( null, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );

		assertThatResult( query.fetchAll() ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( index.typeName(), docId( i ) );
					}
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3389")
	public void maxResults_zero() {
		assertThatResult( matchAllQuerySortByField().fetch( 0, 0 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasNoHits();
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuerySortByField() {
		StubMappingScope scope = index.createScope();
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "integer" ).asc() );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuerySortByDefault() {
		StubMappingScope scope = index.createScope();
		return scope.query().where( f -> f.simpleQueryString().field( "text" )
				.matching( "mostimportantword^100 lessimportantword^10 leastimportantword^0.1" ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchFirstHalfQuery() {
		StubMappingScope scope = index.createScope();
		return scope.query()
				.where( f -> f.range().field( "integer" ).lessThan( DOCUMENT_COUNT / 2 ) )
				.sort( f -> f.field( "integer" ).asc() );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchOneQuery(int id) {
		StubMappingScope scope = index.createScope();
		return scope.query()
				.where( f -> f.match().field( "integer" ).matching( id ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchNoneQuery() {
		StubMappingScope scope = index.createScope();
		return scope.query()
				.where( f -> f.match().field( "integer" ).matching( DOCUMENT_COUNT + 2 ) );
	}

	private void initData() {
		index.bulkIndexer()
				.add( DOCUMENT_COUNT, i -> documentProvider(
						docId( i ),
						document -> {
							// Ensure strictly decreasing score for tests relying on score sort,
							// at least for the first three documents.
							String text = null;
							switch ( i ) {
								case 0:
									text = "leastimportantword lessimportantword mostimportantword";
									break;
								case 1:
									text = "leastimportantword lessimportantword";
									break;
								default:
									text = "leastimportantword";
									break;
							}
							document.addValue( index.binding().text, text );
							document.addValue( index.binding().integer, i );
						}
				) )
				.join();
	}

	private static String docId(int i) {
		return String.format( Locale.ROOT, "document_%05d", i );
	}

	private static class IndexBinding {
		final IndexFieldReference<String> text;
		final IndexFieldReference<Integer> integer;

		IndexBinding(IndexSchemaElement root) {
			text = root.field( "text", f -> f.asString()
					.analyzer( DefaultAnalysisDefinitions.ANALYZER_STANDARD_ENGLISH.name ) )
					.toReference();
			integer = root.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) )
					.toReference();
		}
	}
}
