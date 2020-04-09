/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.search;

import static org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMapperUtils.referenceProvider;

import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.integrationtest.backend.tck.testsupport.configuration.DefaultAnalysisDefinitions;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.assertj.core.api.Assertions;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class ElasticsearchMatchSearchPredicateIT {

	private static final String INDEX_NAME = "IndexName";
	private static final String TEST_TERM = "ThisWillBeLowercasedByTheNormalizer";

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
	public void match_skipAnalysis_normalizedStringField() {
		Assertions.assertThatThrownBy( () -> indexManager.createScope().query()
				.where( f -> f.match().field( "normalizedStringField" ).matching( TEST_TERM ).skipAnalysis() )
				.toQuery()
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining( "HSEARCH400560" )
				.hasMessageContaining( "Elasticsearch backend does not support skip analysis on not analyzed field" )
				.hasMessageContaining( "normalizedStringField" );
	}

	private void initData() {
		IndexIndexingPlan<?> plan = indexManager.createIndexingPlan();
		plan.add( referenceProvider( "1" ), document -> document.addValue( indexMapping.normalizedStringField, TEST_TERM ) );
		plan.execute().join();
	}

	private static class IndexMapping {
		final IndexFieldReference<String> normalizedStringField;

		IndexMapping(IndexSchemaElement root) {
			normalizedStringField = root.field(
					"normalizedStringField",
					c -> c.asString().normalizer( DefaultAnalysisDefinitions.NORMALIZER_LOWERCASE.name )
			)
					.toReference();
		}
	}
}
