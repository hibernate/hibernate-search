/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresIndexOpenClose;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexAdminAnalyzerITAnalysisConfigurer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * Tests related to analyzers when updating indexes.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionMigrationIT")
@Category(RequiresIndexOpenClose.class)
public class ElasticsearchIndexUpdateAnalyzerIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void nothingToDo() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace'],"
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types']"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
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

		setup();

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'type': 'custom',"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									/*
									 * Strangely enough, even if you send properly typed booleans
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of booleans...
									 */
									+ "'generate_word_parts': 'false'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
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

		setup();

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'type': 'custom',"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									/*
									 * Strangely enough, even if you send properly typed booleans
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of booleans...
									 */
									+ "'generate_word_parts': 'false'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void analyzer_componentDefinition_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					/*
					 * We don't add the analyzer here: since a component is missing
					 * the analyzer can't reference it and thus it must be missing too.
					 */
					// missing: 'char_filter'
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

		setup();

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'type': 'custom',"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									/*
									 * Strangely enough, even if you send properly typed booleans
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of booleans...
									 */
									+ "'generate_word_parts': 'false'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void analyzer_componentReference_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['html_strip']," // Invalid
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

		setup();

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'type': 'custom',"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									/*
									 * Strangely enough, even if you send properly typed booleans
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of booleans...
									 */
									+ "'generate_word_parts': 'false'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void analyzer_componentDefinition_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'char_filter': ['custom-pattern-replace']," // Correct, but the actual definition is not
									+ "'tokenizer': 'custom-edgeNGram',"
									+ "'filter': ['custom-keep-types', 'custom-word-delimiter']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-pattern-replace': {"
									+ "'type': 'html_strip'" // Invalid
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

		setup();

		assertJsonEquals(
				"{"
					+ "'analyzer': {"
							+ "'custom-analyzer': {"
									+ "'type': 'custom',"
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
									 */
									+ "'min_gram': '1',"
									+ "'max_gram': '10'"
							+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-keep-types': {"
									+ "'type': 'keep_types',"
									+ "'types': ['<NUM>', '<DOUBLE>']"
							+ "},"
							+ "'custom-word-delimiter': {"
									+ "'type': 'word_delimiter',"
									/*
									 * Strangely enough, even if you send properly typed booleans
									 * to Elasticsearch, when you ask for the current settings it
									 * will spit back strings instead of booleans...
									 */
									+ "'generate_word_parts': 'false'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	private void setup() {
		startSetupWithLifecycleStrategy()
				.withIndex(
						INDEX_NAME,
						ctx -> { }
				)
				.setup();
	}

	private SearchSetupHelper.SetupContext startSetupWithLifecycleStrategy() {
		return setupHelper.start( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.UPDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexAdminAnalyzerITAnalysisConfigurer()
				);
	}

}
