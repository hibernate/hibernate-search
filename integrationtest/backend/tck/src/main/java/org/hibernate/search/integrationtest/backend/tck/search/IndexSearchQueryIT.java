/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchScope;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class IndexSearchQueryIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String STRING_1 = "aaa";

	private static final String DOCUMENT_2 = "2";
	private static final String STRING_2 = "bbb";

	private static final String DOCUMENT_3 = "3";
	private static final String STRING_3 = "ccc";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

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
	public void paging() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).asc() )
				.toQuery();

		assertThat( query.fetch( null, 1L ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).asc() )
				.toQuery();

		assertThat( query.fetch( 1L, 1L ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).asc() )
				.toQuery();

		assertThat( query.fetch( 2L, null ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).asc() )
				.toQuery();

		assertThat( query.fetch( null, null ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void paging_reuse_query() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).asc() )
				.toQuery();

		assertThat( query.fetch( null, 1L ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		assertThat( query.fetch( 1L, 1L ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_2 );

		assertThat( query.fetch( 2L, null ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		assertThat( query.fetch( null, null ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );

		assertThat( query.fetch() ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasDocRefHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3389")
	public void maxResults_zero() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.sort( c -> c.byField( "string" ).asc() )
				.toQuery();

		assertThat( query.fetch( 0L, 0L ) ).fromQuery( query )
				.hasTotalHitCount( 3 )
				.hasNoHits();
	}

	@Test
	public void getQueryString() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.match().onField( "string" ).matching( "platypus" ) )
				.toQuery();

		assertThat( query.getQueryString() ).contains( "platypus" );
	}

	@Test
	public void queryWrapper() {
		StubMappingSearchScope scope = indexManager.createSearchScope();

		QueryWrapper queryWrapper = scope.query()
				.asReference( QueryWrapper::new )
				.predicate( f -> f.match().onField( "string" ).matching( "platypus" ) )
				.toQuery();
		assertThat( queryWrapper.query.getQueryString() ).contains( "platypus" );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			document.addValue( indexMapping.string, STRING_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			document.addValue( indexMapping.string, STRING_2 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			document.addValue( indexMapping.string, STRING_3 );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		StubMappingSearchScope scope = indexManager.createSearchScope();
		IndexSearchQuery<DocumentReference> query = scope.query()
				.asReference()
				.predicate( f -> f.matchAll() )
				.toQuery();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> string;

		IndexMapping(IndexSchemaElement root) {
			string = root.field( "string", f -> f.asString().sortable( Sortable.YES ) )
					.toReference();
		}
	}

	private static class QueryWrapper {
		private final IndexSearchQuery<?> query;

		private QueryWrapper(IndexSearchQuery<?> query) {
			this.query = query;
		}
	}
}
