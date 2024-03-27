/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to normalizers when validating indexes,
 * for all index-validating schema management operations.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchNormalizerDefinitionValidationIT")
class ElasticsearchIndexSchemaManagerValidationNormalizerIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_simple(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
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

		setupAndValidate( operation );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void normalizer_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
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
						.failure( "Missing normalizer" ),
				operation
		);
	}

	private void setupAndValidateExpectingFailure(FailureReportChecker failureReportChecker,
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( failureReportChecker );
	}

	private void setupAndValidate(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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
