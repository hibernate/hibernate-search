/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexStatus;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests for the index status checks,
 * for all status-checking schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-2456")
class ElasticsearchIndexSchemaManagerStatusCheckIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.statusChecking().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@BeforeEach
	public void checkAssumptions() {
		assumeTrue(
				ElasticsearchTckBackendFeatures.supportsIndexStatusCheck(),
				"This test only makes sense if the backend supports index status checks"
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void indexMissing(ElasticsearchIndexSchemaManagerOperation operation) throws Exception {
		assumeFalse( ElasticsearchIndexSchemaManagerOperation.creating().contains( operation ),
				"The operation " + operation + " creates an index automatically."
						+ " No point running this test."
		);

		elasticSearchClient.index( index.name() ).ensureDoesNotExist();

		assertThatThrownBy( () -> setupAndInspectIndex( "index-settings-for-tests/5-replicas.json", operation ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400050" );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void invalidIndexStatus_creatingIndex(ElasticsearchIndexSchemaManagerOperation operation) throws Exception {
		assumeTrue(
				ElasticsearchIndexSchemaManagerOperation.creating().contains( operation ),
				"The operation " + operation + " doesn't create an index automatically."
						+ " No point running this test."
		);

		elasticSearchClient.index( index.name() ).ensureDoesNotExist();

		// Make sure automatically created indexes will never be green by requiring 5 replicas
		// (more than the amount of ES nodes)
		assertThatThrownBy( () -> setupAndInspectIndex( "index-settings-for-tests/5-replicas.json", operation ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400024", "100ms" );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void invalidIndexStatus_usingPreexistingIndex(ElasticsearchIndexSchemaManagerOperation operation) throws Exception {
		assumeFalse(
				ElasticsearchIndexSchemaManagerOperation.dropping().contains( operation ),
				"The operation " + operation + " drops the existing index automatically."
						+ " No point running this test."
		);

		// Make sure automatically created indexes will never be green by requiring 5 replicas
		// (more than the amount of ES nodes)
		elasticSearchClient.index( index.name() )
				.deleteAndCreate( "number_of_replicas", "5" )
				.type().putMapping(
						simpleMappingForInitialization( "" )
				);

		assertThatThrownBy( () -> setupAndInspectIndex( "index-settings-for-tests/5-replicas.json", operation ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "HSEARCH400024", "100ms" );
	}


	private void setupAndInspectIndex(String settingsPath, ElasticsearchIndexSchemaManagerOperation operation) {
		setupHelper.start()
				.withBackendProperty(
						// Don't contribute any analysis definitions, validation of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty( ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE, settingsPath )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS,
						IndexStatus.GREEN.externalRepresentation()
				)
				.withBackendProperty(
						ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MINIMAL_REQUIRED_STATUS_WAIT_TIMEOUT,
						"100"
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

}
