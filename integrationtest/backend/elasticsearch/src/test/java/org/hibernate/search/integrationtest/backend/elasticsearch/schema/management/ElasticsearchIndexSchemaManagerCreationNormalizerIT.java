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
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.configuration.ElasticsearchIndexSchemaManagerNormalizerITAnalysisConfigurer;
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
 * Tests related to normalizers when creating indexes,
 * for all index-creating schema management operations.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.ElasticsearchAnalyzerDefinitionCreationIT")
public class ElasticsearchIndexSchemaManagerCreationNormalizerIT {

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

	public ElasticsearchIndexSchemaManagerCreationNormalizerIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_simple() {
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

	@Test
	public void success_multiIndex() {
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
