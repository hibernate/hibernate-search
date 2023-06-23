/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
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

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> operations() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final ElasticsearchIndexSchemaManagerValidationOperation operation;

	public ElasticsearchIndexSchemaManagerValidationNormalizerIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidate();

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void normalizer_missing() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.normalizerContext( "custom-normalizer" )
						.failure( "Missing normalizer" )
		);
	}

	private void setupAndValidateExpectingFailure(FailureReportChecker failureReportChecker) {
		assertThatThrownBy( this::setupAndValidate )
				.isInstanceOf( SearchException.class )
				.satisfies( failureReportChecker );
	}

	private void setupAndValidate() {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer()
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

	protected void putMapping() {
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);
	}

}
