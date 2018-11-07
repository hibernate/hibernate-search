/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.search.predicate;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldAccessor;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingSearchTarget;
import org.hibernate.search.integrationtest.backend.tck.util.rule.SearchSetupHelper;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchQuery;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MatchAllSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";

	private static final String DOCUMENT_1 = "1";
	private static final String STRING_1 = "aaa";

	private static final String DOCUMENT_2 = "2";
	private static final String STRING_2 = "bbb";

	private static final String DOCUMENT_3 = "3";
	private static final String STRING_3 = "ccc";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexAccessors indexAccessors;
	private StubMappingIndexManager indexManager;

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
	public void matchAll() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	@Test
	public void matchAll_except() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().except( c2 -> c2.match().onField( "string" ).matching( STRING_1 ).toPredicate() ).toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_2, DOCUMENT_3 );

		SearchPredicate searchPredicate = searchTarget.predicate().match().onField( "string" ).matching( STRING_2 ).toPredicate();
		query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().except( searchPredicate ).toPredicate() )
				.build();
		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_3 );
	}

	@Test
	public void matchAll_multipleExcepts() {
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();

		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll()
						.except( f.match().onField( "string" ).matching( STRING_1 ) )
						.except( f.match().onField( "string" ).matching( STRING_2 ) )
						.toPredicate()
				)
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_3 );

		SearchPredicate searchPredicate1 = searchTarget.predicate().match().onField( "string" ).matching( STRING_3 ).toPredicate();
		SearchPredicate searchPredicate2 = searchTarget.predicate().match().onField( "string" ).matching( STRING_2 ).toPredicate();

		query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().except( searchPredicate1 ).except( searchPredicate2 ).toPredicate() )
				.build();

		assertThat( query )
				.hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1 );
	}

	private void initData() {
		IndexWorkPlan<? extends DocumentElement> workPlan = indexManager.createWorkPlan();
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
		StubMappingSearchTarget searchTarget = indexManager.createSearchTarget();
		SearchQuery<DocumentReference> query = searchTarget.query()
				.asReferences()
				.predicate( f -> f.matchAll().toPredicate() )
				.build();
		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, DOCUMENT_1, DOCUMENT_2, DOCUMENT_3 );
	}

	private static class IndexAccessors {
		final IndexFieldAccessor<String> string;

		IndexAccessors(IndexSchemaElement root) {
			string = root.field( "string" ).asString().createAccessor();
		}
	}
}
