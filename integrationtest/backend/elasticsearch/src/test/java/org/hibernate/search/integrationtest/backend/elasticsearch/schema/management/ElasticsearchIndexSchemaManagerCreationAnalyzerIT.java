/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerAnalyzerITAnalysisConfigurer;
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
 * Tests related to analyzers when creating indexes,
 * for all index-creating schema management operations.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionCreationIT")
public class ElasticsearchIndexSchemaManagerCreationAnalyzerIT {

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final StubMappedIndex mainIndex = StubMappedIndex.withoutFields().name( "main" );
	private final StubMappedIndex otherIndex = StubMappedIndex.withoutFields().name( "other" );

	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerCreationAnalyzerIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_simple() {
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

	@Test
	public void success_multiIndex() {
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
