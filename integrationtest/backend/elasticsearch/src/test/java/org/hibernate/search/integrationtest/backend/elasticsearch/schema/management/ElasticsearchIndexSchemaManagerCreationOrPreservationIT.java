/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to preserving a pre-existing index,
 * for all schema management operations that create missing indexes but leave existing indexes as-is.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchSchemaCreateStrategyIT")
public class ElasticsearchIndexSchemaManagerCreationOrPreservationIT {

	private static final String INDEX_NAME = "IndexName";

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creatingOrPreserving();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	private StubMappingIndexManager indexManager;

	public ElasticsearchIndexSchemaManagerCreationOrPreservationIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2789")
	public void alreadyExists() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'field': {"
								+ "'type': 'keyword'"
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'date'"
						+ "}"
				)
		);

		assertJsonEquals(
				simpleMappingForExpectations(
						"'field': {"
								+ "'type': 'keyword'"
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'date'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);

		setupAndCreateIndexIfMissingOnly();

		// The mapping should be unchanged
		assertJsonEquals(
				simpleMappingForExpectations(
						"'field': {"
								+ "'type': 'keyword'"
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'date'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	@TestForIssue(jiraKey = "HSEARCH-2789")
	public void doesNotExist() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndexIfMissingOnly();

		// Just check that *something* changed
		// Other test classes check that the changes actually make sense
		assertJsonEqualsIgnoringUnknownFields(
				"{ 'properties': { 'field': { } } }",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	private void setupAndCreateIndexIfMissingOnly() {
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
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( indexManager.getSchemaManager() ) );
	}

}
