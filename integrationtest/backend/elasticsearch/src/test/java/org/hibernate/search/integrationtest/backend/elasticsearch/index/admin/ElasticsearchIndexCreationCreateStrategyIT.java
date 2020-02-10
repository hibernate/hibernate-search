/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for aspects of the CREATE index lifecycle strategy
 * that are not addressed in other tests
 * (because not applicable to other index-creating strategies).
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchSchemaCreateStrategyIT")
public class ElasticsearchIndexCreationCreateStrategyIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2789")
	public void alreadyExists() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();

		assertJsonEquals(
				"{ }",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);

		setup();

		assertJsonEquals(
				"{ }",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2789")
	public void doesNotExist() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup();

		// Just check that *something* changed
		// Other test classes check that the changes actually make sense
		assertJsonEqualsIgnoringUnknownFields(
				"{ 'properties': { 'field': { } } }",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	private void setup() {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "field", f -> f.asString() )
									.toReference();
						}
				)
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.CREATE.getExternalRepresentation()
				)
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();
	}

}
