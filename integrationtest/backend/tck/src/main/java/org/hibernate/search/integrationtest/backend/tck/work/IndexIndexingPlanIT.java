/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.tck.work;

import static org.hibernate.search.util.impl.integrationtest.common.assertion.SearchResultAssert.assertThat;
import static org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.DocumentElement;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.common.DocumentReference;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.common.stub.mapper.StubMappingIndexManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class IndexIndexingPlanIT {

	private static final String INDEX_NAME = "indexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	private IndexMapping indexMapping;
	private StubMappingIndexManager indexManager;

	@Before
	public void before() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> this.indexMapping = new IndexMapping( ctx.getSchemaElement() ),
						indexManager -> this.indexManager = indexManager
				)
				.setup();
	}

	@Test
	public void discard() {
		IndexIndexingPlan<? extends DocumentElement> plan = indexManager.createIndexingPlan();

		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.title, "Title of Book 1" ) );
		plan.discard();

		plan.add( referenceProvider( "2" ), document -> document.addValue( indexMapping.title, "Title of Book 2" ) );
		plan.execute().join();

		SearchQuery<DocumentReference> query = indexManager.createScope().query()
				.predicate( f -> f.matchAll() )
				.toQuery();

		assertThat( query ).hasDocRefHitsAnyOrder( INDEX_NAME, "2" );
	}

	private static class IndexMapping {
		final IndexFieldReference<String> title;

		IndexMapping(IndexSchemaElement root) {
			title = root.field( "name", f -> f.asString() ).toReference();
		}
	}
}
