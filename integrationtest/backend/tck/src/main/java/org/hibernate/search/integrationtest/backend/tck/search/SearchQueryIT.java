/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.backend.index.spi.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexSearchTarget;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.util.impl.integrationtest.common.assertion.DocumentReferencesSearchResultAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class SearchQueryIT {

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

	private IndexAccessors indexAccessors;
	private IndexManager<?> indexManager;
	private SessionContext sessionContext = new StubSessionContext();

	@Before
	public void setup() {
		setupHelper.withDefaultConfiguration()
				.withIndex(
						"MappedType", INDEX_NAME,
						ctx -> this.indexAccessors = new IndexAccessors( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();

		initData();
	}

	@Test
	public void paging() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( "string" ).asc().end()
				.build();
		query.setFirstResult( 1L );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( "string" ).asc().end()
				.build();
		query.setFirstResult( 1L );
		query.setMaxResults( 1L );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_2 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( "string" ).asc().end()
				.build();
		query.setMaxResults( 2L );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( "string" ).asc().end()
				.build();
		query.setFirstResult( null );
		query.setMaxResults( null );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void paging_reuse_query() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( "string" ).asc().end()
				.build();
		query.setFirstResult( 1L );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		query.setFirstResult( 1L );
		query.setMaxResults( 1L );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_2 );

		query.setFirstResult( null );
		query.setMaxResults( 2L );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2 );

		query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.sort().byField( "string" ).asc().end()
				.build();
		query.setFirstResult( null );
		query.setMaxResults( null );

		DocumentReferencesSearchResultAssert.assertThat( query )
				.hasHitCount( 3 )
				.hasReferencesHitsExactOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void getQueryString() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().match().onField( "string" ).matching( "platypus" ).end()
				.build();

		assertThat( query.getQueryString() ).contains( "platypus" );
	}

	@Test
	public void asWrappedQuery() {
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();

		QueryWrapper queryWrapper = searchTarget.query( sessionContext )
				.asReferences()
				.asWrappedQuery( q -> new QueryWrapper( q ) )
				.predicate().match().onField( "string" ).matching( "platypus" ).end()
				.build();

		assertThat( queryWrapper.query.getQueryString() ).contains( "platypus" );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan( sessionContext );
		workPlan.add( referenceProvider( DOCUMENT_1 ), document -> {
			indexAccessors.string.write( document, STRING_1 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_2 ), document -> {
			indexAccessors.string.write( document, STRING_2 );
		} );
		workPlan.add( referenceProvider( DOCUMENT_3 ), document -> {
			indexAccessors.string.write( document, STRING_3 );
		} );

		workPlan.execute().join();

		// Check that all documents are searchable
		IndexSearchTarget searchTarget = indexManager.createSearchTarget().build();
		SearchQuery<DocumentReference> query = searchTarget.query( sessionContext )
				.asReferences()
				.predicate().matchAll().end()
				.build();
		DocumentReferencesSearchResultAssert.assertThat( query ).hasReferencesHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().sortable( Sortable.YES ).createAccessor();
		}
	}

	private static class QueryWrapper {

		private SearchQuery<?> query;

		private QueryWrapper(SearchQuery<?> query) {
			this.query = query;
		}
	}
}
