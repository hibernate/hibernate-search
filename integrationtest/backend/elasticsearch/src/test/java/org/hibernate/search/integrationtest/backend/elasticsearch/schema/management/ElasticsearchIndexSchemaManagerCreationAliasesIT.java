/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleReadAliasDefinition;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleWriteAliasDefinition;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.encodeName;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.StubSingleIndexLayoutStrategy;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to aliases when creating indexes,
 * for all index-creating schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
class ElasticsearchIndexSchemaManagerCreationAliasesIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.creating().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_defaultLayoutStrategy(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( null, operation );

		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( index.name() ) + "': " + simpleWriteAliasDefinition() + ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_noAliasLayoutStrategy(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.indexNoAlias( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( "no-alias", operation );

		assertJsonEquals(
				"{"
						+ "}",
				elasticsearchClient.indexNoAlias( index.name() ).aliases().get()
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_customLayoutStrategy(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		setupAndCreateIndex( new StubSingleIndexLayoutStrategy( "custom-write", "custom-read" ), operation );

		assertJsonEquals(
				"{"
						+ "'custom-write': " + simpleWriteAliasDefinition() + ", "
						+ "  'custom-read': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
	}

	/**
	 * Test that migrating from 6.0.0.Beta4 or earlier will just create new (empty) indexes,
	 * keeping the old ones in place.
	 */
	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void migrationFrom6Beta4OrEarlier(ElasticsearchIndexSchemaManagerOperation operation) {
		// Index layout of 6.0.0.Beta4 and before: aliases are missing,
		// and the primary Elasticsearch index name is just the Hibernate Search index name.
		URLEncodedString oldIndexName = encodeName( index.name() );
		elasticsearchClient.index( oldIndexName, null, null )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );

		setupAndCreateIndex( null, operation );

		// New indexes are created
		assertJsonEquals(
				"{"
						+ "'" + defaultWriteAlias( index.name() ) + "': " + simpleWriteAliasDefinition() + ", "
						+ "  '" + defaultReadAlias( index.name() ) + "': " + simpleReadAliasDefinition()
						+ "}",
				elasticsearchClient.index( index.name() ).aliases().get()
		);
		// Old indexes are still there: we expect users to reindex and delete old indexes.
		assertJsonEquals(
				"{}",
				elasticsearchClient.index( oldIndexName, null, null ).aliases().get()
		);
	}

	private void setupAndCreateIndex(Object layoutStrategy, ElasticsearchIndexSchemaManagerOperation operation) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty( ElasticsearchBackendSettings.LAYOUT_STRATEGY, layoutStrategy )
				.withIndex( index )
				.setup();

		operation.apply( index.schemaManager() ).join();
	}

}
