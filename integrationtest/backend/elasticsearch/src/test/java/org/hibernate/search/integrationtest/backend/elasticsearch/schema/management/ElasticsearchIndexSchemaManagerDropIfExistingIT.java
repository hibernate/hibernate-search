/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to dropping the index.
 */
public class ElasticsearchIndexSchemaManagerDropIfExistingIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private StubMappingIndexManager indexManager;

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void alreadyExists() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'field': {"
								+ "'type': 'keyword'"
						+ "},"
						+ "'NOTmyField': {"
								+ "'type': 'date'"
						+ "}"
				)
		);

		assertThat( elasticSearchClient.index( INDEX_NAME ).exists() ).isTrue();

		setupAndDropIndexIfExisting();

		assertThat( elasticSearchClient.index( INDEX_NAME ).exists() ).isFalse();
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-3759")
	public void doesNotExist() {
		elasticSearchClient.index( INDEX_NAME ).ensureDoesNotExist();

		assertThat( elasticSearchClient.index( INDEX_NAME ).exists() ).isFalse();

		setupAndDropIndexIfExisting();

		// Nothing should have happened, and we should get here without throwing an exception
		assertThat( elasticSearchClient.index( INDEX_NAME ).exists() ).isFalse();
	}

	private void setupAndDropIndexIfExisting() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "field", f -> f.asString() )
									.toReference();
						},
						indexManager -> this.indexManager = indexManager
				)
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.NONE
				)
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		Futures.unwrappedExceptionJoin( indexManager.getSchemaManager().dropIfExisting() );
	}

}
