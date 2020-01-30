/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.management.ElasticsearchManagementTestUtils.simpleMappingForInitialization;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchAnalyzerManagementITAnalysisConfigurer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the analyzer validation feature when using automatic index management.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionValidationIT")
public class ElasticsearchAnalyzerDefinitionValidationIT {

	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									/*
									 * Strangely enough, even if you send properly typed numbers
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of numbers...
									 * Testing both a properly typed number and a number as string here.
									 */
									+ "'min_gram': 1,"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									/*
									 * The order doesn't matter with this array.
									 * Here we test that validation ignores order.
									 */
									+ "'types': ['<DOUBLE>', '<NUM>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setup();

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void analyzer_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.build()
		);
	}

	@Test
	public void analyzer_charFilters_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['html_strip'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure(
								"Invalid char filters. Expected '[custom-pattern-replace]',"
										+ " actual is '[html_strip]'"
						)
						.build()
		);
	}

	@Test
	public void analyzer_tokenizer_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'whitespace',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure(
								"Invalid tokenizer. Expected 'custom-edgeNGram',"
										+ " actual is 'whitespace'"
						)
						.build()
		);
	}

	@Test
	public void analyzer_tokenFilters_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['standard', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure(
								"Invalid token filters. Expected '[custom-keep-types, custom-word-delimiter]',"
										+ " actual is '[standard, custom-word-delimiter]'"
						)
						.build()
		);
	}

	@Test
	public void charFilter_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.charFilterContext( "custom-pattern-replace" )
						.failure( "Missing char filter" )
						.build()
		);
	}

	@Test
	public void tokenizer_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.tokenizerContext( "custom-edgeNGram" )
						.failure( "Missing tokenizer" )
						.build()
		);
	}

	@Test
	public void tokenFilter_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.analyzerContext( "custom-analyzer" )
						.failure( "Missing analyzer" )
						.tokenFilterContext( "custom-keep-types" )
						.failure( "Missing token filter" )
						.build()
		);
	}

	@Test
	public void charFilter_type_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'html_strip',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.charFilterContext( "custom-pattern-replace" )
						.failure(
								"Invalid type. Expected 'pattern_replace', actual is 'html_strip'"
						)
						.build()
		);
	}

	@Test
	public void charFilter_parameter_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^a-z]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.charFilterContext( "custom-pattern-replace" )
						.analysisDefinitionParameterContext( "pattern" )
						.failure(
								"Invalid value. Expected '\"[^0-9]\"', actual is '\"[^a-z]\"'"
						)
						.build()
		);
	}

	@Test
	public void charFilter_parameter_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0'"
									// Missing "tags"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.charFilterContext( "custom-pattern-replace" )
						.analysisDefinitionParameterContext( "tags" )
						.failure(
								"Invalid value. Expected '\"CASE_INSENSITIVE|COMMENTS\"', actual is 'null'"
						)
						.build()
		);
	}

	@Test
	public void tokenFilter_parameter_unexpected() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'pattern_replace',"
									+ "'pattern': '[^0-9]',"
									+ "'replacement': '0',"
									+ "'tags': 'CASE_INSENSITIVE|COMMENTS'"
							+ "}"
					+ "},"
					+ "'tokenizer': {"
							+ "'custom-edgeNGram': {"
									+ "'type': 'edgeNGram',"
									+ "'min_gram': 1,"
									+ "'max_gram': 10"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									+ "'generate_word_parts': false,"
									+ "'generate_number_parts': false"
							+ "}"
					+ "}"
				+ "}"
				);

		putMapping();

		setupExpectingFailure(
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.tokenFilterContext( "custom-word-delimiter" )
						.analysisDefinitionParameterContext( "generate_number_parts" )
						.failure(
								"Invalid value. Expected 'null', actual is '\"false\"'"
						)
						.build()
		);
	}

	private void setupExpectingFailure(String failureReportPattern) {
		SubTest.expectException( this::setup )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( failureReportPattern );
	}

	private void setup() {
		withManagementStrategyConfiguration()
				.withIndex(
						INDEX_NAME,
						ctx -> { }
				)
				.setup();
	}



	private SearchSetupHelper.SetupContext withManagementStrategyConfiguration() {
		return setupHelper.start( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.VALIDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchAnalyzerManagementITAnalysisConfigurer()
				);
	}

	protected void putMapping() {
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization( "" )
		);
	}
}
