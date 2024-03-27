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
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to analyzers when validating indexes,
 * for all index-validating schema management operations.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionValidationIT")
class ElasticsearchIndexSchemaManagerValidationAnalyzerIT {

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
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						/*
						 * Strangely enough, even if you send properly typed numbers
						 * to Elasticsearch, when you ask for the current settings it
						 * will spit back strings instead of numbers...
						 * Testing both a properly typed number and a number as string here.
						 */
						+ "     'min_gram': 1,"
						+ "     'max_gram': '10'"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						/*
						 * The order doesn't matter with this array.
						 * Here we test that validation ignores order.
						 */
						+ "     'types': ['<DOUBLE>', '<NUM>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
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
	void analyzer_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" ),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void analyzer_charFilters_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['html_strip'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure(
								"Invalid char filters. Expected '[custom-pattern-replace]',"
										+ " actual is '[html_strip]'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void analyzer_tokenizer_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'whitespace',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure(
								"Invalid tokenizer. Expected 'custom-edgeNGram',"
										+ " actual is 'whitespace'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void analyzer_tokenFilters_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['lowercase', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure(
								"Invalid token filters. Expected '[custom-keep-types, custom-word-delimiter]',"
										+ " actual is '[lowercase, custom-word-delimiter]'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void charFilter_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.charFilterContext( "custom-pattern-replace" )
						.failure( "Missing char filter" ),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void tokenizer_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.tokenizerContext( "custom-edgeNGram" )
						.failure( "Missing tokenizer" ),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void tokenFilter_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.tokenFilterContext( "custom-keep-types" )
						.failure( "Missing token filter" ),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void charFilter_type_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'html_strip',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.charFilterContext( "custom-pattern-replace" )
						.failure(
								"Invalid type. Expected 'pattern_replace', actual is 'html_strip'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void charFilter_parameter_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^a-z]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.charFilterContext( "custom-pattern-replace" )
						.analysisDefinitionParameterContext( "pattern" )
						.failure(
								"Invalid value. Expected '\"[^0-9]\"', actual is '\"[^a-z]\"'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void charFilter_parameter_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0'"
						// Missing "tags"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				FailureReportUtils.hasFailureReport()
						.charFilterContext( "custom-pattern-replace" )
						.analysisDefinitionParameterContext( "tags" )
						.failure(
								"Invalid value. Expected '\"CASE_INSENSITIVE|COMMENTS\"', actual is 'null'"
						),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void tokenFilter_parameter_unexpected(ElasticsearchIndexSchemaManagerValidationOperation operation)
			throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						+ "     'min_gram': 1,"
						+ "     'max_gram': 10"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						+ "     'generate_word_parts': false,"
						+ "     'generate_number_parts': false"
						+ "   }"
						+ " }"
						+ "}"
		);

		putMapping();

		setupAndValidateExpectingFailure(
				hasValidationFailureReport()
						.tokenFilterContext( "custom-word-delimiter" )
						.analysisDefinitionParameterContext( "generate_number_parts" )
						.failure(
								"Invalid value. Expected 'null', actual is '\"false\"'"
						),
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
						new ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer()
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
