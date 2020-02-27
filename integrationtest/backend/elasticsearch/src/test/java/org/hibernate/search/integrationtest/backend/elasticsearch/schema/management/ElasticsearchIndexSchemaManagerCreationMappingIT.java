/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexBindingContext;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to the mapping when creating indexes,
 * for all index-creating schema management operations.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaCreationIT")
public class ElasticsearchIndexSchemaManagerCreationMappingIT {

	private static final String INDEX_NAME = "IndexName";

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	private StubMappingIndexManager indexManager;

	public ElasticsearchIndexSchemaManagerCreationMappingIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@After
	public void cleanUp() {
		if ( indexManager != null ) {
			indexManager.getSchemaManager().dropIfExisting();
		}
	}

	@Test
	public void dateField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asLocalDate()
			)
					.toReference();
		} );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'date',"
								+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
								+ "'doc_values': false"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void booleanField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asBoolean()
			)
					.toReference();
		} );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'boolean',"
								+ "'doc_values': false"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void keywordField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asString()
			)
					.toReference();
		} );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'keyword',"
								+ "'doc_values': false"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void textField() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asString().analyzer( "standard" )
			)
					.toReference();
		} );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'text',"
								+ "'analyzer': 'standard'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void textField_noNorms() {
		elasticSearchClient.index( INDEX_NAME )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( ctx -> {
			ctx.getSchemaElement().field(
					"myField",
					f -> f.asString().analyzer( "standard" ).norms( Norms.NO )
			)
					.toReference();
		} );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'text',"
								+ "'analyzer': 'standard',"
								+ "'norms': false"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	private void setupAndCreateIndex(Consumer<IndexBindingContext> mappingContributor) {
		setupHelper.start()
				.withIndex(
						INDEX_NAME,
						mappingContributor,
						indexManager -> this.indexManager = indexManager
				)
				.withIndexDefaultsProperty(
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.NONE
				)
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		operation.apply( indexManager.getSchemaManager() ).join();
	}

}
