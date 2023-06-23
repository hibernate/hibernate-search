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

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to index custom settings when validating indexes,
 * for all index-validating schema management operations.
 */
@RunWith(Parameterized.class)
public class ElasticsearchIndexSchemaManagerValidationCustomSettingsIT {

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> operations() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final ElasticsearchIndexSchemaManagerValidationOperation operation;

	public ElasticsearchIndexSchemaManagerValidationCustomSettingsIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_simple() {
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

		setupAndValidate();

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void invalid_analysis() {
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
						)
		);
	}

	@Test
	public void invalid_numberOfShards() {
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
						)
		);
	}

	@Test
	public void invalid_maxResultWindow() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ 'max_result_window': '20000' }" );

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		assertThatThrownBy( () -> setupAndValidate( "max-result-window.json" ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexSettingsCustomAttributeContext( "max_result_window" )
						.failure(
								"Invalid value. Expected '250', actual is '20000'"
						) );
	}

	@Test
	public void default_maxResultWindow() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ 'max_result_window': '10000' }" );

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidate( null );
		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void empty() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ }" );

		elasticsearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization( "" )
		);

		setupAndValidate( null );
		// If we get here, it means validation passed (no exception was thrown)
	}

	private void setupAndValidateExpectingFailure(FailureReportChecker failureReportChecker) {
		assertThatThrownBy( this::setupAndValidate )
				.isInstanceOf( SearchException.class )
				.satisfies( failureReportChecker );
	}

	private void setupAndValidate() {
		setupAndValidate( "valid.json" );
	}

	private void setupAndValidate(String customSettingsFile) {
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
