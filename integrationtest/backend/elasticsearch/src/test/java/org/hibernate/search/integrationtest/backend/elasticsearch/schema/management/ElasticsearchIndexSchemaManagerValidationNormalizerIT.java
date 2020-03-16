/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to normalizers when validating indexes,
 * for all index-validating schema management operations.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchNormalizerDefinitionValidationIT")
public class ElasticsearchIndexSchemaManagerValidationNormalizerIT {

	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";

	private static final String INDEX_NAME = "IndexName";

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> operations() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerValidationOperation operation;

	private StubMappingIndexManager indexManager;

	public ElasticsearchIndexSchemaManagerValidationNormalizerIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'char_filter': ['custom-char-mapping'],"
									+ "'filter': ['custom-elision']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupAndValidate();

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void normalizer_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupAndValidateExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.normalizerContext( "custom-normalizer" )
						.failure( "Missing normalizer" )
						.build()
		);
	}

	private void setupAndValidateExpectingFailure(String failureReportPattern) {
		SubTest.expectException( this::setupAndValidate )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( failureReportPattern );
	}

	private void setupAndValidate() {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer()
				)
				.withIndex( INDEX_NAME, ctx -> { }, indexManager -> this.indexManager = indexManager )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( indexManager.getSchemaManager() ) );
	}

	protected void putMapping() {
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization( "" )
		);
	}

}
