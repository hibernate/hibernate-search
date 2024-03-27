/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultReadAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultWriteAlias;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.writeAliasDefinition;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
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
 * Tests related to aliases when inspecting existing indexes,
 * for all alias-inspecting schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-3791")
class ElasticsearchIndexSchemaManagerInspectionAliasesIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.aliasInspecting().stream()
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
	void writeAliasTargetsMultipleIndexes(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate();
		// The write alias for index 1 also targets a second index
		elasticsearchClient.index( "otherIndex" )
				.deleteAndCreate()
				.aliases().put( defaultWriteAlias( index.name() ).original );

		assertThatThrownBy(
				() -> setupAndInspectIndex( operation )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch index layout",
						"index names [" + defaultWriteAlias( index.name() ) + ", " + defaultReadAlias( index.name() )
								+ "] resolve to multiple distinct indexes ["
								+ defaultPrimaryName( index.name() ) + ", "
								+ defaultPrimaryName( "otherIndex" ) + "]",
						"These names must resolve to a single index" );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void readAliasTargetsMultipleIndexes(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate();
		// The read alias for index 1 also targets a second index
		elasticsearchClient.index( "otherIndex" )
				.deleteAndCreate()
				.aliases().put( defaultReadAlias( index.name() ).original );

		assertThatThrownBy(
				() -> setupAndInspectIndex( operation )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch index layout",
						"index names [" + defaultWriteAlias( index.name() ) + ", " + defaultReadAlias( index.name() )
								+ "] resolve to multiple distinct indexes ["
								+ defaultPrimaryName( index.name() ) + ", "
								+ defaultPrimaryName( "otherIndex" ) + "]",
						"These names must resolve to a single index" );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void readAndWriteAliasTargetDifferentIndexes(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.deleteAndCreate();
		// The write alias for index 1 actually targets a different index
		elasticsearchClient.index( "otherIndex" )
				.deleteAndCreate();
		elasticsearchClient.index( index.name() )
				.aliases().move( defaultWriteAlias( index.name() ).original,
						defaultPrimaryName( "otherIndex" ).original, writeAliasDefinition() );

		assertThatThrownBy( () -> setupAndInspectIndex( operation ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "Invalid Elasticsearch index layout",
						"index names [" + defaultWriteAlias( index.name() ) + ", " + defaultReadAlias( index.name() )
								+ "] resolve to multiple distinct indexes ["
								+ defaultPrimaryName( index.name() ) + ", "
								+ defaultPrimaryName( "otherIndex" ) + "]",
						"These names must resolve to a single index" );
	}

	private void setupAndInspectIndex(ElasticsearchIndexSchemaManagerOperation operation) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

}
