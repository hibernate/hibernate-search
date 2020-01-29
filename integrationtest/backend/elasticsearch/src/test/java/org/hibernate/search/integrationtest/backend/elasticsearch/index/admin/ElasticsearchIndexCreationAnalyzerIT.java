/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.index.admin;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexAdminAnalyzerITAnalysisConfigurer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to analyzers when creating indexes,
 * for all applicable index lifecycle strategies.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionCreationIT")
public class ElasticsearchIndexCreationAnalyzerIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With strategy {0}")
	public static EnumSet<IndexLifecycleStrategyName> strategies() {
		return EnumSet.complementOf( EnumSet.of(
				// Those strategies don't create the schema, so we don't test them
				IndexLifecycleStrategyName.NONE, IndexLifecycleStrategyName.VALIDATE
		) );
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final IndexLifecycleStrategyName strategy;

	public ElasticsearchIndexCreationAnalyzerIT(IndexLifecycleStrategyName strategy) {
		super();
		this.strategy = strategy;
	}

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

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
						strategy.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexAdminAnalyzerITAnalysisConfigurer()
				);
	}

}
