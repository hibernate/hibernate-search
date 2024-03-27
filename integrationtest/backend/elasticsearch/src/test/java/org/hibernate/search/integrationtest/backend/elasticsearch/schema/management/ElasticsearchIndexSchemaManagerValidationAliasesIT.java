/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleAliasDefinition;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to aliases when validating indexes,
 * for all index-validating schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
class ElasticsearchIndexSchemaManagerValidationAliasesIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all().stream()
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
	void success_defaultLayoutStrategy(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		setupAndValidate( operation );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_noAliasLayoutStrategy(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.indexNoAlias( index.name() )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.indexNoAlias( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		setupAndValidate( "no-alias", operation );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void writeAlias_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( defaultPrimaryName( index.name() ), null, defaultReadAlias( index.name() ) )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies(
						hasValidationFailureReport()
								.aliasContext( defaultWriteAlias( index.name() ).original )
								.failure( "Missing alias" )
				);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void writeAlias_invalid_filter(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( index.name() ).aliases()
				.put(
						defaultWriteAlias( index.name() ).original,
						simpleAliasDefinition( true, "'filter': {'term': {'user_id': 12}}" )
				);

		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies(
						hasValidationFailureReport()
								.aliasContext( defaultWriteAlias( index.name() ).original )
								.aliasAttributeContext( "filter" )
								.failure( "Invalid value. Expected 'null', actual is '{\"term\":{\"user_id\":12}}'" )
				);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void writeAlias_invalid_isWriteIndex(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( index.name() ).aliases()
				.put( defaultWriteAlias( index.name() ).original, simpleAliasDefinition( false, "" ) );

		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies(
						hasValidationFailureReport()
								.aliasContext( defaultWriteAlias( index.name() ).original )
								.aliasAttributeContext( "is_write_index" )
								.failure( "Invalid value. Expected 'true', actual is 'false'" )
				);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void readAlias_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( defaultPrimaryName( index.name() ), defaultWriteAlias( index.name() ), null )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );

		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies(
						hasValidationFailureReport()
								.aliasContext( defaultReadAlias( index.name() ).original )
								.failure( "Missing alias" )
				);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void readAlias_invalid_filter(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping( simpleMappingForInitialization( "" ) );
		elasticsearchClient.index( index.name() ).aliases()
				.put( "somePreExistingAlias" );
		elasticsearchClient.index( index.name() ).aliases()
				.put( defaultReadAlias( index.name() ).original, "{'filter': {'term': {'user_id': 12}}}" );

		assertThatThrownBy( () -> setupAndValidate( operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies(
						hasValidationFailureReport()
								.aliasContext( defaultReadAlias( index.name() ).original )
								.aliasAttributeContext( "filter" )
								.failure( "Invalid value. Expected 'null', actual is '{\"term\":{\"user_id\":12}}'" )
				);
	}

	private void setupAndValidate(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		setupAndValidate( null, operation );
	}

	private void setupAndValidate(Object layoutStrategy, ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

}
