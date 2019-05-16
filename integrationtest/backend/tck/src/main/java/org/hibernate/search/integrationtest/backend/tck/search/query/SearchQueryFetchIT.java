/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalizeReference;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.assertj.core.api.Assertions;

public class SearchQueryFetchIT {

	private static final String INDEX_NAME = "IndexName";
	private static final int DOCUMENT_COUNT = 200;

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void fetch_noArg() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetch() ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		query = matchFirstHalfQuery();
		assertThat( query.fetch() ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT / 2 )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT / 2; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	public void fetch_limit() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetch( null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		query = matchAllQuery();
		assertThat( query.fetch( 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ) );

		query = matchAllQuery();
		assertThat( query.fetch( 2 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );
	}

	@Test
	public void fetch_limitAndOffset() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetch( null, 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		query = matchAllQuery();
		assertThat( query.fetch( 1, 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 1 ) );

		query = matchAllQuery();
		assertThat( query.fetch( 2, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );

		query = matchAllQuery();
		assertThat( query.fetch( null, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	public void fetchHits_noArg() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetchHits() ).fromQuery( query )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		query = matchFirstHalfQuery();
		assertThat( query.fetchHits() ).fromQuery( query )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT / 2; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	public void fetchHits_limit() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetchHits( null ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		query = matchAllQuery();
		assertThat( query.fetchHits( 1 ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ) );

		query = matchAllQuery();
		assertThat( query.fetchHits( 2 ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );
	}

	@Test
	public void fetchHits_limitAndOffset() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetchHits( null, 1 ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		query = matchAllQuery();
		assertThat( query.fetchHits( 1, 1 ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 1 ) );

		query = matchAllQuery();
		assertThat( query.fetchHits( 2, null ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );

		query = matchAllQuery();
		assertThat( query.fetchHits( null, null ) ).fromQuery( query )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	public void fetchTotalHitCount() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		Assertions.assertThat( query.fetchTotalHitCount() ).isEqualTo( DOCUMENT_COUNT );

		query = matchFirstHalfQuery();
		Assertions.assertThat( query.fetchTotalHitCount() ).isEqualTo( DOCUMENT_COUNT / 2 );
	}

	@Test
	public void fetchSingleHit() {
		SearchQuery<DocumentReference> query = matchOneQuery( 4 );

		Optional<DocumentReference> result = query.fetchSingleHit();
		Assertions.assertThat( result ).isNotEmpty();
		Assertions.assertThat( normalizeReference( result.get() ) )
				.isEqualTo( normalizeReference( reference( INDEX_NAME, docId( 4 ) ) ) );

		query = matchNoneQuery();
		result = query.fetchSingleHit();
		Assertions.assertThat( result ).isEmpty();

		SubTest.expectException( () -> {
			SearchQuery<DocumentReference> matchAllQuery = matchAllQuery();
			matchAllQuery.fetchSingleHit();
		} )
				.assertThrown()
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void fetch_limitAndOffset_reuseQuery() {
		SearchQuery<DocumentReference> query = matchAllQuery();
		assertThat( query.fetch( null, 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( query.fetch( 1, 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 1 ) );

		assertThat( query.fetch( 2, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );

		assertThat( query.fetch( null, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( query.fetch() ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3389")
	public void maxResults_zero() {
		SearchQuery<DocumentReference> query = matchAllQuery();

		assertThat( query.fetch( 0, 0 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasNoHits();
	}

	private SearchQuery<DocumentReference> matchAllQuery() {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		return scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "integer" ).asc() )
				.toQuery();
	}

	private SearchQuery<DocumentReference> matchFirstHalfQuery() {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		return scope.query()
				.asReference()
				.predicate( f -> f.range().onField( "integer" ).below( DOCUMENT_COUNT / 2 ).excludeLimit() )
				.sort( c -> c.byField( "integer" ).asc() )
				.toQuery();
	}

	private SearchQuery<DocumentReference> matchOneQuery(int id) {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		return scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "integer" ).matching( id ) )
				.toQuery();
	}

	private SearchQuery<DocumentReference> matchNoneQuery() {
		StubMappingSearchScope scope = indexManager.createSearchScope();
		return scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "integer" ).matching( DOCUMENT_COUNT + 2 ) )
				.toQuery();
	}

	private DocumentReference[] references(int first, int afterLast) {
		List<DocumentReference> references = new ArrayList<>();
		for ( int i = first; i < afterLast; i++ ) {
			references.add( reference( INDEX_NAME, docId( i ) ) );
		}
		return references.toArray( new DocumentReference[0] );
	}

	private void initData() {
		IndexDocumentWorkExecutor<? extends DocumentElement> executor = indexManager.createDocumentWorkExecutor();
		List<CompletableFuture<?>> futures = new ArrayList<>();
		for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
			int intValue = i;
			futures.add( executor.add( referenceProvider( docId( i ) ), document -> {
				document.addValue( indexMapping.integer, intValue );
			} ) );
		}

		CompletableFuture.allOf( futures.toArray( new CompletableFuture<?>[0] ) ).join();
		indexManager.createWorkExecutor().flush().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		SearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasTotalHitCount( DOCUMENT_COUNT );
	}

	private static String docId(int i) {
		return String.format( Locale.ROOT, "document_%05d", i );
	}

	private static class IndexMapping {
		final IndexFieldReference<Integer> integer;

		IndexMapping(IndexSchemaElement root) {
			integer = root.field( "integer", f -> f.asInteger().sortable( Sortable.YES ) )
					.toReference();
		}
	}

	private static class QueryWrapper {
		private final SearchQuery<?> query;

		private QueryWrapper(SearchQuery<?> query) {
			this.query = query;
		}
	}
}
