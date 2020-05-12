/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.query;

import static org.hibernate.search.util.impl.integrationtest.common.NormalizationUtils.normalize;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.backend.StubBackendUtils.reference;
import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.documentProvider;

import java.util.Locale;
import java.util.Optional;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingScope;
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
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void fetchAll() {
		assertThat( matchAllQuery().fetchAll() )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( matchFirstHalfQuery().fetchAll() )
				.hasTotalHitCount( DOCUMENT_COUNT / 2 )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT / 2; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	public void fetch_limit() {
		assertThat( matchAllQuery().fetch( null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( matchAllQuery().fetch( 1 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ) );

		assertThat( matchAllQuery().fetch( 2 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );
	}

	@Test
	public void fetch_offset_limit() {
		assertThat( matchAllQuery().fetch( 1, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( matchAllQuery().fetch( 1, 1 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 1 ) );

		assertThat( matchAllQuery().fetch( null, 2 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );

		assertThat( matchAllQuery().fetch( null, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		// Fetch beyond the total hit count
		assertThat( matchAllQuery().fetch( DOCUMENT_COUNT + 1, null ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasNoHits();
	}

	@Test
	public void fetchAllHits() {
		assertThat( matchAllQuery().fetchAllHits() )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( matchFirstHalfQuery().fetchAllHits() )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT / 2; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );
	}

	@Test
	public void fetchHits_limit() {
		assertThat( matchAllQuery().fetchHits( null ) )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( matchAllQuery().fetchHits( 1 ) )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ) );

		assertThat( matchAllQuery().fetchHits( 2 ) )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );
	}

	@Test
	public void fetchHits_offset_limit() {
		assertThat( matchAllQuery().fetchHits( 1, null ) )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( matchAllQuery().fetchHits( 1, 1 ) )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 1 ) );

		assertThat( matchAllQuery().fetchHits( null, 2 ) )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );

		assertThat( matchAllQuery().fetchHits( null, null ) )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		// Fetch beyond the total hit count
		assertThat( matchAllQuery().fetchHits( DOCUMENT_COUNT + 1, null ) )
				.isEmpty();
	}

	@Test
	public void fetchTotalHitCount() {
		Assertions.assertThat( matchAllQuery().fetchTotalHitCount() ).isEqualTo( DOCUMENT_COUNT );

		Assertions.assertThat( matchFirstHalfQuery().fetchTotalHitCount() ).isEqualTo( DOCUMENT_COUNT / 2 );
	}

	@Test
	public void fetchSingleHit() {
		Optional<DocumentReference> result = matchOneQuery( 4 ).fetchSingleHit();
		Assertions.assertThat( result ).isNotEmpty();
		Assertions.assertThat( normalize( result.get() ) )
				.isEqualTo( normalize( reference( INDEX_NAME, docId( 4 ) ) ) );

		result = matchNoneQuery().fetchSingleHit();
		Assertions.assertThat( result ).isEmpty();

		Assertions.assertThatThrownBy( () -> {
			matchAllQuery().fetchSingleHit();
		} )
				.isInstanceOf( SearchException.class );
	}

	@Test
	public void fetch_limitAndOffset_reuseQuery() {
		SearchQuery<DocumentReference> query = matchAllQuery().toQuery();
		assertThat( query.fetch( 1, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 1; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( query.fetch( 1, 1 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 1 ) );

		assertThat( query.fetch( null, 2 ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( INDEX_NAME, docId( 0 ), docId( 1 ) );

		assertThat( query.fetch( null, null ) ).fromQuery( query )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasDocRefHitsExactOrder( builder -> {
					for ( int i = 0; i < DOCUMENT_COUNT; i++ ) {
						builder.doc( INDEX_NAME, docId( i ) );
					}
				} );

		assertThat( query.fetchAll() ).fromQuery( query )
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
		assertThat( matchAllQuery().fetch( 0, 0 ) )
				.hasTotalHitCount( DOCUMENT_COUNT )
				.hasNoHits();
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchAllQuery() {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.matchAll() )
				.sort( f -> f.field( "integer" ).asc() );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchFirstHalfQuery() {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.range().field( "integer" ).lessThan( DOCUMENT_COUNT / 2 ) )
				.sort( f -> f.field( "integer" ).asc() );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchOneQuery(int id) {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.match().field( "integer" ).matching( id ) );
	}

	private SearchQueryOptionsStep<?, DocumentReference, ?, ?, ?> matchNoneQuery() {
		StubMappingScope scope = indexManager.createScope();
		return scope.query()
				.where( f -> f.match().field( "integer" ).matching( DOCUMENT_COUNT + 2 ) );
	}

	private void initData() {
		indexManager.initAsync(
				DOCUMENT_COUNT, i -> documentProvider(
						docId( i ),
						document -> document.addValue( indexMapping.integer, i )
				)
		).join();

		// Check that all documents are searchable
		StubMappingScope scope = indexManager.createScope();
		SearchQuery<DocumentReference> query = scope.query()
				.where( f -> f.matchAll() )
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
}
