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
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to analyzers when validating indexes,
 * for all index-validating schema management operations.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionValidationIT")
public class ElasticsearchIndexSchemaManagerValidationAnalyzerIT {

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

	public ElasticsearchIndexSchemaManagerValidationAnalyzerIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_simple() throws Exception {
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

		setupAndValidate();

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void analyzer_missing() throws Exception {
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
						.failure( "Missing analyzer" )
		);
	}

	@Test
	public void analyzer_charFilters_invalid() throws Exception {
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
						)
		);
	}

	@Test
	public void analyzer_tokenizer_invalid() throws Exception {
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
						)
		);
	}

	@Test
	public void analyzer_tokenFilters_invalid() throws Exception {
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
						)
		);
	}

	@Test
	public void charFilter_missing() throws Exception {
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
						.failure( "Missing char filter" )
		);
	}

	@Test
	public void tokenizer_missing() throws Exception {
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
						.failure( "Missing tokenizer" )
		);
	}

	@Test
	public void tokenFilter_missing() throws Exception {
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
						.failure( "Missing token filter" )
		);
	}

	@Test
	public void charFilter_type_invalid() throws Exception {
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
						)
		);
	}

	@Test
	public void charFilter_parameter_invalid() throws Exception {
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
						)
		);
	}

	@Test
	public void charFilter_parameter_missing() throws Exception {
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
						)
		);
	}

	@Test
	public void tokenFilter_parameter_unexpected() throws Exception {
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
						)
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
