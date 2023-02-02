/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.junit.Assume.assumeFalse;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to normalizers when updating indexes.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchNormalizerDefinitionMigrationIT")
public class ElasticsearchIndexSchemaManagerUpdateNormalizerIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Before
	public void checkAssumption() {
		assumeFalse(
				"This test only is only relevant if we are allowed to open/close Elasticsearch indexes." +
						" These operations are not available on AWS in particular.",
				ElasticsearchTestHostConnectionConfiguration.get().isAws()
		);
	}

	@Test
	public void nothingToDo() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
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

		setupAndUpdateIndex();

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
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_missing() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
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

		setupAndUpdateIndex();

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
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentDefinition_missing() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
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

		setupAndUpdateIndex();

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
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentReference_invalid() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
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

		setupAndUpdateIndex();

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
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
				);
	}

	@Test
	public void normalizer_componentDefinition_invalid() throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate(
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

		setupAndUpdateIndex();

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
				elasticSearchClient.index( index.name() ).settings( "index.analysis" ).get()
				);
	}

	private void setupAndUpdateIndex() {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer()
				)
				.withIndex( index )
				.setup();

		index.schemaManager().createOrUpdate( OperationSubmitter.blocking() ).join();
	}

}
