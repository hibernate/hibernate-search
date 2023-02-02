/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to dropping, then creating the index.
 */
public class ElasticsearchIndexSchemaManagerDropAndCreateIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
		root.field( "field", f -> f.asString() )
				.toReference()
	);

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void alreadyExists() {
		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'field': {"
								+ "'type': 'text'"
						+ "},"
						+ "'NOTmyField': {"
								+ "'type': 'date'"
						+ "}"
				)
		);

		assertJsonEquals(
				simpleMappingForExpectations(
						"'field': {"
								+ "'type': 'text'"
						+ "},"
						+ "'NOTmyField': {"
								+ "'type': 'date'"
						+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);

		setupAndDropAndCreateIndex();

		// Expect the index to have been re-created
		assertJsonEquals(
				simpleMappingForExpectations(
						// Previous index was dropped => new schema
						"'field': {"
								+ "'type': 'keyword',"
								+ "'doc_values': false"
						+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void doesNotExist() {
		elasticSearchClient.index( index.name() ).ensureDoesNotExist();

		assertThat( elasticSearchClient.index( index.name() ).exists() ).isFalse();

		setupAndDropAndCreateIndex();

		// Expect the index to have been created
		assertJsonEquals(
				simpleMappingForExpectations(
						"'field': {"
								+ "'type': 'keyword',"
								+ "'doc_values': false"
						+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	private void setupAndDropAndCreateIndex() {
		setupHelper.start()
				.withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		Futures.unwrappedExceptionJoin(
				index.schemaManager().dropAndCreate( OperationSubmitter.blocking() )
		);
	}

}
