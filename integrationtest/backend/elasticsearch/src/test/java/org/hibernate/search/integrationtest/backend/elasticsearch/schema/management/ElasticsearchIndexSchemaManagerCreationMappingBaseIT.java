/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

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
public class ElasticsearchIndexSchemaManagerCreationMappingBaseIT {

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerCreationMappingBaseIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void dateField() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "myField", f -> f.asLocalDate() )
				.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "  'type': 'date',"
								+ "  'format': '"
								+ elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat() + "',"
								+ "  'doc_values': false"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void booleanField() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "myField", f -> f.asBoolean() )
				.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "  'type': 'boolean',"
								+ "  'doc_values': false"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void keywordField() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "myField", f -> f.asString() )
				.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "  'type': 'keyword',"
								+ "  'doc_values': false"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void textField() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "standard" ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'analyzer': 'standard'"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void textField_noNorms() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "standard" ).norms( Norms.NO ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index );

		assertJsonEquals(
				ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'analyzer': 'standard',"
								+ "  'norms': false"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	private void setupAndCreateIndex(StubMappedIndex index) {
		setupHelper.start()
				.withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		operation.apply( index.schemaManager() ).join();
	}

}
