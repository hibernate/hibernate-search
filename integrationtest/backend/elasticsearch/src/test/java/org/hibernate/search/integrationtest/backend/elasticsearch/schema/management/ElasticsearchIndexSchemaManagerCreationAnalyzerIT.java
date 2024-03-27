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
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer;
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
 * Tests related to analyzers when creating indexes,
 * for all index-creating schema management operations.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionCreationIT")
class ElasticsearchIndexSchemaManagerCreationAnalyzerIT {

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
						new ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer() )
				.withIndex( mainIndex )
				.setup();

		operation.apply( mainIndex.schemaManager() ).join();

		assertJsonEquals(
				"{"
						+ " 'analyzer': {"
						+ "   'custom-analyzer': {"
						+ "     'type': 'custom',"
						+ "     'char_filter': ['custom-pattern-replace'],"
						+ "     'tokenizer': 'custom-edgeNGram',"
						+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
						+ "   }"
						+ " },"
						+ " 'char_filter': {"
						+ "   'custom-pattern-replace': {"
						+ "     'type': 'pattern_replace',"
						+ "     'pattern': '[^0-9]',"
						+ "     'replacement': '0',"
						+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
						+ "   }"
						+ " },"
						+ " 'tokenizer': {"
						+ "   'custom-edgeNGram': {"
						+ "     'type': 'edge_ngram',"
						/*
						 * Strangely enough, even if you send properly typed numbers
						 * to Elasticsearch, when you ask for the current settings it
						 * will spit back strings instead of numbers...
						 */
						+ "     'min_gram': '1',"
						+ "     'max_gram': '10'"
						+ "   }"
						+ " },"
						+ " 'filter': {"
						+ "   'custom-keep-types': {"
						+ "     'type': 'keep_types',"
						+ "     'types': ['<NUM>', '<DOUBLE>']"
						+ "   },"
						+ "   'custom-word-delimiter': {"
						+ "     'type': 'word_delimiter',"
						/*
						 * Strangely enough, even if you send properly typed booleans
						 * to Elasticsearch, when you ask for the current settings it
						 * will spit back strings instead of booleans...
						 */
						+ "     'generate_word_parts': 'false'"
						+ "   }"
						+ " }"
						+ "}",
				elasticSearchClient.index( mainIndex.name() ).settings( "index.analysis" ).get()
		);
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
						new ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer() )
				.withIndexes( mainIndex, otherIndex )
				.withIndexProperty( otherIndex.name(), ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) context -> {
							// Use a different definition for custom-analyzer
							context.analyzer( "custom-analyzer" ).custom()
									.tokenizer( "whitespace" )
									.tokenFilters( "lowercase", "asciifolding" );
							// Add an extra analyzer
							context.analyzer( "custom-analyzer-2" ).custom()
									.tokenizer( "whitespace" )
									.tokenFilters( "lowercase" );
						} )
				.setup();

		CompletableFuture.allOf(
				operation.apply( mainIndex.schemaManager() ),
				operation.apply( otherIndex.schemaManager() )
		)
				.join();

		assertJsonEquals( "{"
				+ " 'analyzer': {"
				+ "   'custom-analyzer': {"
				+ "     'type': 'custom',"
				+ "     'char_filter': ['custom-pattern-replace'],"
				+ "     'tokenizer': 'custom-edgeNGram',"
				+ "     'filter': ['custom-keep-types', 'custom-word-delimiter']"
				+ "   }"
				+ " },"
				+ " 'char_filter': {"
				+ "   'custom-pattern-replace': {"
				+ "     'type': 'pattern_replace',"
				+ "     'pattern': '[^0-9]',"
				+ "     'replacement': '0',"
				+ "     'tags': 'CASE_INSENSITIVE|COMMENTS'"
				+ "   }"
				+ " },"
				+ " 'tokenizer': {"
				+ "   'custom-edgeNGram': {"
				+ "     'type': 'edge_ngram',"
				/*
				 * Strangely enough, even if you send properly typed numbers
				 * to Elasticsearch, when you ask for the current settings it
				 * will spit back strings instead of numbers...
				 */
				+ "     'min_gram': '1',"
				+ "     'max_gram': '10'"
				+ "   }"
				+ " },"
				+ " 'filter': {"
				+ "   'custom-keep-types': {"
				+ "     'type': 'keep_types',"
				+ "     'types': ['<NUM>', '<DOUBLE>']"
				+ "   },"
				+ "   'custom-word-delimiter': {"
				+ "     'type': 'word_delimiter',"
				/*
				 * Strangely enough, even if you send properly typed booleans
				 * to Elasticsearch, when you ask for the current settings it
				 * will spit back strings instead of booleans...
				 */
				+ "     'generate_word_parts': 'false'"
				+ "   }"
				+ " }"
				+ "}",
				elasticSearchClient.index( mainIndex.name() ).settings( "index.analysis" ).get() );

		assertJsonEquals( "{"
				+ " 'analyzer': {"
				+ "   'custom-analyzer': {"
				+ "     'type': 'custom',"
				+ "     'tokenizer': 'whitespace',"
				+ "     'filter': ['lowercase', 'asciifolding']"
				+ "   },"
				+ "   'custom-analyzer-2': {"
				+ "     'type': 'custom',"
				+ "     'tokenizer': 'whitespace',"
				+ "     'filter': ['lowercase']"
				+ "   }"
				// elements defined in the default configurer shouldn't appear here: they've been overridden
				+ " }"
				+ "}",
				elasticSearchClient.index( otherIndex.name() ).settings( "index.analysis" ).get() );
	}

}
