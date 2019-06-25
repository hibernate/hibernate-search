/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests for the schema creation feature when using automatic index management, for all applicable strategies.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaCreationIT")
public class ElasticsearchSchemaCreationIT {

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With strategy {0}")
	public static EnumSet<ElasticsearchIndexLifecycleStrategyName> strategies() {
		return EnumSet.complementOf( EnumSet.of(
				// Those strategies don't create the schema, so we don't test them
				ElasticsearchIndexLifecycleStrategyName.NONE, ElasticsearchIndexLifecycleStrategyName.VALIDATE
				) );
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexLifecycleStrategyName strategy;

	public ElasticsearchSchemaCreationIT(ElasticsearchIndexLifecycleStrategyName strategy) {
		super();
		this.strategy = strategy;
	}

	@Test
	public void dateField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asLocalDate()
			)
					.toReference();
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
									+ "'doc_values': false"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
				);
	}

	@Test
	public void booleanField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asBoolean()
			)
					.toReference();
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
				);
	}

	@Test
	public void keywordField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asString()
			)
					.toReference();
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'keyword',"
									+ "'doc_values': false"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
				);
	}

	@Test
	public void textField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asString().analyzer( "standard" )
			)
					.toReference();
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'standard'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
				);
	}

	@Test
	public void textField_noNorms() {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setup( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asString().analyzer( "standard" ).norms( Norms.NO )
			)
					.toReference();
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'analyzer': 'standard',"
									+ "'norms': false"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
				);
	}

	private void setup(Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndex(
						INDEX_NAME,
						mappingContributor
				)
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						strategy.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchAnalysisConfigurer() {
							@Override
							public void configure(ElasticsearchAnalysisConfigurationContext context) {
								// No-op
							}
						}
				)
				.setup();
	}

}
