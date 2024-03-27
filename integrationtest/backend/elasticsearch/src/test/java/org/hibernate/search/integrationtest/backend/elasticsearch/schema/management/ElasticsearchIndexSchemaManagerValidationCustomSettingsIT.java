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

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to index custom settings when validating indexes,
 * for all index-validating schema management operations.
 */
class ElasticsearchIndexSchemaManagerValidationCustomSettingsIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_simple(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index",
				" { " +
						"   'number_of_shards': '3', " +
						"   'number_of_replicas': '3', " +
						"   'analysis': { " +
						"     'analyzer': { " +
						"       'my_standard-english': { " +
						"         'type': 'standard', " +
						"         'stopwords': '_english_' " +
						"       }, " +
						"       'my_analyzer_ngram': { " +
						"         'type': 'custom', " +
						"         'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"       } " +
						"     }, " +
						"     'tokenizer': { " +
						"       'my_analyzer_ngram_tokenizer': { " +
						"         'type': 'ngram', " +
						"         'min_gram': '5', " +
						"         'max_gram': '6' " +
						"       } " +
						"     } " +
						"   } " +
						" } "
		);

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidate( operation );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void invalid_analysis(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index",
				" { " +
						"   'number_of_shards': '3', " +
						"   'number_of_replicas': '3', " +
						"   'analysis': { " +
						"     'analyzer': { " +
						"       'my_standard-english': { " +
						"         'type': 'standard', " +
						"         'stopwords': '_english_' " +
						"       }, " +
						"       'my_analyzer_ngram': { " +
						"         'type': 'custom', " +
						"         'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"       } " +
						"     }, " +
						"     'tokenizer': { " +
						"       'my_analyzer_ngram_tokenizer': { " +
						"         'type': 'ngram', " +
						"         'min_gram': '2', " +
						"         'max_gram': '3' " +
						"       } " +
						"     } " +
						"   } " +
						" } "
		);

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.tokenizerContext( "my_analyzer_ngram_tokenizer" )
						.analysisDefinitionParameterContext( "min_gram" )
						.failure(
								"Invalid value. Expected '\"5\"', actual is '\"2\"'"
						)
						.analysisDefinitionParameterContext( "max_gram" )
						.failure(
								"Invalid value. Expected '\"6\"', actual is '\"3\"'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void invalid_numberOfShards(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index",
				" { " +
						"   'number_of_shards': '7', " +
						"   'number_of_replicas': '3', " +
						"   'analysis': { " +
						"     'analyzer': { " +
						"       'my_standard-english': { " +
						"         'type': 'standard', " +
						"         'stopwords': '_english_' " +
						"       }, " +
						"       'my_analyzer_ngram': { " +
						"         'type': 'custom', " +
						"         'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"       } " +
						"     }, " +
						"     'tokenizer': { " +
						"       'my_analyzer_ngram_tokenizer': { " +
						"         'type': 'ngram', " +
						"         'min_gram': '5', " +
						"         'max_gram': '6' " +
						"       } " +
						"     } " +
						"   } " +
						" } "
		);

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.indexSettingsCustomAttributeContext( "number_of_shards" )
						.failure(
								"Invalid value. Expected '\"3\"', actual is '\"7\"'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void invalid_maxResultWindow(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ 'max_result_window': '20000' }" );

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		assertThatThrownBy( () -> setupAndValidate( "max-result-window.json", operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexSettingsCustomAttributeContext( "max_result_window" )
						.failure(
								"Invalid value. Expected '250', actual is '20000'"
						) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void default_maxResultWindow(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ 'max_result_window': '10000' }" );

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidate( null, operation );
		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void empty(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ }" );

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidate( null, operation );
		// If we get here, it means validation passed (no exception was thrown)
	}

	private void setupAndValidateExpectingFailure(FailureReportChecker failureReportChecker,
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( failureReportChecker );
	}

	private void setupAndValidate(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		setupAndValidate( "valid.json", operation );
	}

	private void setupAndValidate(String customSettingsFile, ElasticsearchIndexSchemaManagerValidationOperation operation) {
		SearchSetupHelper.SetupContext setupContext = setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// use an empty analysis configurer,
						// so that we have only the custom settings definitions
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndex( index );

		if ( customSettingsFile != null ) {
			setupContext
					.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
							"custom-index-settings/" + customSettingsFile
					);
		}

		setupContext.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}
}
