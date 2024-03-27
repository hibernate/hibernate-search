/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to the mapping when creating indexes,
 * for all index-creating schema management operations.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaCreationIT")
class ElasticsearchIndexSchemaManagerCreationMappingBaseIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.creating().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void dateField(ElasticsearchIndexSchemaManagerOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "myField", f -> f.asLocalDate() )
				.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index, operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void booleanField(ElasticsearchIndexSchemaManagerOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "myField", f -> f.asBoolean() )
				.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index, operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void keywordField(ElasticsearchIndexSchemaManagerOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "myField", f -> f.asString() )
				.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index, operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void textField(ElasticsearchIndexSchemaManagerOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "standard" ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index, operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void textField_noNorms(ElasticsearchIndexSchemaManagerOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "standard" ).norms( Norms.NO ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( index, operation );

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

	private void setupAndCreateIndex(StubMappedIndex index, ElasticsearchIndexSchemaManagerOperation operation) {
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
