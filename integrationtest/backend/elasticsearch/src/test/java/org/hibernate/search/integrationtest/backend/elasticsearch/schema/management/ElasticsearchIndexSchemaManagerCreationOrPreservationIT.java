/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to preserving a pre-existing index,
 * for all schema management operations that create missing indexes but leave existing indexes as-is.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchSchemaCreateStrategyIT")
class ElasticsearchIndexSchemaManagerCreationOrPreservationIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.creatingOrPreserving().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> root.field( "field", f -> f.asString() )
			.toReference()
	);

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-2789")
	void alreadyExists(ElasticsearchIndexSchemaManagerOperation operation) throws Exception {
		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'field': {"
								+ "  'type': 'keyword'"
								+ "},"
								+ "'NOTmyField': {" // Ignored during validation
								+ "  'type': 'date'"
								+ "}"
				)
		);

		assertJsonEquals(
				simpleMappingForExpectations(
						"'field': {"
								+ "  'type': 'keyword'"
								+ "},"
								+ "'NOTmyField': {" // Ignored during validation
								+ "  'type': 'date'"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);

		setupAndCreateIndexIfMissingOnly( operation );

		// The mapping should be unchanged
		assertJsonEquals(
				simpleMappingForExpectations(
						"'field': {"
								+ "  'type': 'keyword'"
								+ "},"
								+ "'NOTmyField': {" // Ignored during validation
								+ "  'type': 'date'"
								+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-2789")
	void doesNotExist(ElasticsearchIndexSchemaManagerOperation operation) throws Exception {
		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndexIfMissingOnly( operation );

		// Just check that *something* changed
		// Other test classes check that the changes actually make sense
		assertJsonEqualsIgnoringUnknownFields(
				"{ 'properties': { 'field': { } } }",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	private void setupAndCreateIndexIfMissingOnly(ElasticsearchIndexSchemaManagerOperation operation) {
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

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

}
