/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
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
 * Tests related to normalizers when creating indexes,
 * for all index-creating schema management operations.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionCreationIT")
class ElasticsearchIndexSchemaManagerCreationNormalizerIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.creating().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex mainIndex = StubMappedIndex.withoutFields().name( "main" );
	private final StubMappedIndex otherIndex = StubMappedIndex.withoutFields().name( "other" );

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_simple(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticSearchClient.index( mainIndex.name() )
				.ensureDoesNotExist();

		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer() )
				.withIndex( mainIndex )
				.setup();

		operation.apply( mainIndex.schemaManager() ).join();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( mainIndex.name() ).settings( "index.analysis" ).get() );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_multiIndex(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticSearchClient.index( mainIndex.name() )
				.ensureDoesNotExist();
		elasticSearchClient.index( otherIndex.name() )
				.ensureDoesNotExist();

		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer() )
				.withIndexes( mainIndex, otherIndex )
				.withIndexProperty( otherIndex.name(), ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) context -> {
							// Use a different definition for custom-normalizer
							context.normalizer( "custom-normalizer" ).custom()
									.tokenFilters( "lowercase", "asciifolding" );
							// Add an extra normalizer
							context.normalizer( "custom-normalizer-2" ).custom()
									.tokenFilters( "lowercase" );
						} )
				.setup();

		CompletableFuture.allOf(
				operation.apply( mainIndex.schemaManager() ),
				operation.apply( otherIndex.schemaManager() )
		)
				.join();

		assertJsonEquals(
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-char-mapping'],"
						+ "     'filter': ['custom-elision']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-char-mapping': {"
						+ "     'type': 'mapping',"
						+ "     'mappings': ['foo => bar']"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-elision': {"
						+ "     'type': 'elision',"
						+ "     'articles': ['l', 'd']"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( mainIndex.name() ).settings( "index.analysis" ).get() );

		assertJsonEquals( "{"
				+ " 'normalizer': {"
				+ "   'custom-normalizer': {"
				+ "     'type': 'custom',"
				+ "     'filter': ['lowercase', 'asciifolding']"
				+ "   },"
				+ "   'custom-normalizer-2': {"
				+ "     'type': 'custom',"
				+ "     'filter': ['lowercase']"
				+ "   }"
				// elements defined in the default configurer shouldn't appear here: they've been overridden
				+ " }"
				+ "}",
				elasticSearchClient.index( otherIndex.name() ).settings( "index.analysis" ).get() );
	}

}
