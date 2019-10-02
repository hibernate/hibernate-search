/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresIndexOpenClose;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchNormalizerManagementITAnalysisConfigurer;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * Tests for the normalizer migration feature when using automatic index management.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchNormalizerDefinitionMigrationIT")
@Category(RequiresIndexOpenClose.class)
public class ElasticsearchNormalizerDefinitionMigrationIT {

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

		setup();

		assertJsonEquals(
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
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
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

		setup();

		assertJsonEquals(
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'type': 'custom',"
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
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentDefinition_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					/*
					 * We don't add the analyzer here: since a component is missing
					 * the analyzer can't reference it and thus it must be missing too.
					 */
					// missing: 'char_filter'
					+ "'filter': {"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}"
				);

		setup();

		assertJsonEquals(
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'type': 'custom',"
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
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentReference_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'char_filter': ['custom-char-mapping2']," // Invalid
									+ "'filter': ['custom-elision']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "},"
							+ "'custom-char-mapping2': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar2']"
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

		setup();

		assertJsonEquals(
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'type': 'custom',"
									+ "'char_filter': ['custom-char-mapping'],"
									+ "'filter': ['custom-elision']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar']"
							+ "},"
					+ "'custom-char-mapping2': {"
							+ "'type': 'mapping',"
							+ "'mappings': ['foo => bar2']"
					+ "}"
					+ "},"
					+ "'filter': {"
							+ "'custom-elision': {"
									+ "'type': 'elision',"
									+ "'articles': ['l', 'd']"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentDefinition_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis",
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'char_filter': ['custom-char-mapping']," // Correct, but the actual definition is not
									+ "'filter': ['custom-elision']"
							+ "}"
					+ "},"
					+ "'char_filter': {"
							+ "'custom-char-mapping': {"
									+ "'type': 'mapping',"
									+ "'mappings': ['foo => bar2']" // Invalid
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

		setup();

		assertJsonEquals(
				"{"
					+ "'normalizer': {"
							+ "'custom-normalizer': {"
									+ "'type': 'custom',"
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
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).settings( "index.analysis" ).get()
				);
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
						IndexLifecycleStrategyName.UPDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchNormalizerManagementITAnalysisConfigurer()
				);
	}

}
