/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to index custom settings when creating indexes,
 * for all index-creating schema management operations.
 */
@RunWith(Parameterized.class)
@TestForIssue(jiraKey = "HSEARCH-3934")
public class ElasticsearchIndexSchemaManagerCreationCustomSettingsIT {

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerCreationCustomSettingsIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_mergeWithNoOverlapping() {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// merge the default analysis configurer with the custom settings,
		// there are no overlapping of their definitions
		setupAndCreateIndex( null, "custom-index-settings/valid.json" );

		assertJsonEquals(
				" { " +
						"   'analyzer': { " +
						"   	'DefaultAnalysisDefinitions_analyzer_ngram': { " +
						"   		'type': 'custom', " +
						"   		'tokenizer': 'DefaultAnalysisDefinitions_analyzer_ngram_tokenizer' " +
						"   	}, " +
						"   	'my_standard-english': { " +
						"   		'type': 'standard', " +
						"   		'stopwords': '_english_' " +
						"   	}, " +
						"   	'DefaultAnalysisDefinitions_analyzer_whitespace': { " +
						"   		'type': 'custom', " +
						"   		'tokenizer': 'whitespace' " +
						"   	}, " +
						"   	'my_analyzer_ngram': { " +
						"   		'type': 'custom', " +
						"   		'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"   	}, " +
						"   	'DefaultAnalysisDefinitions_analyzer_whitespace_lowercase': { " +
						"   		'type': 'custom', " +
						"   		'tokenizer': 'whitespace', " +
						"   		'filter': ['lowercase'] " +
						"   	}, " +
						"   	'DefaultAnalysisDefinitions_standard-english': { " +
						"   		'type': 'standard', " +
						"   		'stopwords': '_english_' " +
						"   	} " +
						"   }, " +
						"   'normalizer': { " +
						"   	'DefaultAnalysisDefinitions_lowercase': { " +
						"   		'type': 'custom', " +
						"   		'filter': ['lowercase'] " +
						"   	} " +
						"   }, " +
						"   'tokenizer': { " +
						"   	'DefaultAnalysisDefinitions_analyzer_ngram_tokenizer': { " +
						"   		'type': 'ngram', " +
						"   		'min_gram': '5', " +
						"   		'max_gram': '6' " +
						"   	}, " +
						"   	'my_analyzer_ngram_tokenizer': { " +
						"   		'type': 'ngram', " +
						"   		'min_gram': '5', " +
						"   		'max_gram': '6' " +
						"   	} " +
						"   }  " +
						" } ",
				elasticsearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);

		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_shards" ).get() )
				.isEqualTo( "\"3\"" );
		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_replicas" ).get() )
				.isEqualTo( "\"3\"" );
	}

	@Test
	public void success_mergeWithOverlapping() {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// merge the default analysis configurer with the custom settings,
		// there are some overlapping of their definitions
		setupAndCreateIndex( null, "custom-index-settings/overlapping.json" );

		assertJsonEquals(
				" { " +
						" 	'analyzer': { " +
						" 		'DefaultAnalysisDefinitions_analyzer_ngram': { " +
						" 			'type': 'custom', " +
						" 			'tokenizer': 'DefaultAnalysisDefinitions_analyzer_ngram_tokenizer' " +
						" 		}, " +
						" 		'DefaultAnalysisDefinitions_analyzer_whitespace': { " +
						" 			'type': 'custom', " +
						" 			'tokenizer': 'whitespace' " +
						" 		}, " +
						" 		'DefaultAnalysisDefinitions_analyzer_whitespace_lowercase': { " +
						" 			'type': 'custom', " +
						" 			'tokenizer': 'whitespace', " +
						" 			'filter': ['lowercase'] " +
						" 		}, " +
						" 		'DefaultAnalysisDefinitions_standard-english': { " +
						" 			'type': 'standard', " +
						" 			'stopwords': '_english_' " +
						" 		} " +
						" 	}, " +
						" 	'normalizer': { " +
						" 		'DefaultAnalysisDefinitions_lowercase': { " +
						" 			'type': 'custom', " +
						" 			'filter': ['lowercase'] " +
						" 		} " +
						" 	}, " +
						" 	'tokenizer': { " +
						" 		'DefaultAnalysisDefinitions_analyzer_ngram_tokenizer': { " +
						" 			'type': 'ngram', " +
						" 			'min_gram': '7', " +
						" 			'max_gram': '8'" +
						" 		} " +
						" 	} " +
						" } ",
				elasticsearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);

		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_shards" ).get() )
				.isEqualTo( "\"3\"" );
		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_replicas" ).get() )
				.isEqualTo( "\"3\"" );
	}

	@Test
	public void success_onlyCustomSettings() {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// use an empty analysis configurer,
		// so that we have only the custom settings definitions
		setupAndCreateIndex( new EmptyElasticsearchAnalysisConfigurer(), "custom-index-settings/valid.json" );

		assertJsonEquals(
				" { " +
						" 	'analyzer': { " +
						" 		'my_standard-english': { " +
						" 			'type': 'standard', " +
						" 			'stopwords': '_english_' " +
						" 		}, " +
						" 		'my_analyzer_ngram': { " +
						" 			'type': 'custom', " +
						" 			'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						" 		} " +
						" 	}, " +
						" 	'tokenizer': { " +
						" 		'my_analyzer_ngram_tokenizer': { " +
						" 			'type': 'ngram', " +
						" 			'min_gram': '5', " +
						" 			'max_gram': '6' " +
						" 		} " +
						" 	} " +
						" } ",
				elasticsearchClient.index( index.name() ).settings( "index.analysis" ).get()
		);

		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_shards" ).get() )
				.isEqualTo( "\"3\"" );
		assertThat( elasticsearchClient.index( index.name() ).settings( "index.number_of_replicas" ).get() )
				.isEqualTo( "\"3\"" );
	}

	@Test
	public void maxResultWindow() {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// use an empty analysis configurer,
		// so that we have only the custom settings definitions
		setupAndCreateIndex( new EmptyElasticsearchAnalysisConfigurer(), "custom-index-settings/max-result-window.json" );

		assertJsonEquals(
				"\"250\"",
				elasticsearchClient.index( index.name() ).settings( "index.max_result_window" ).get()
		);
	}

	private void setupAndCreateIndex(ElasticsearchAnalysisConfigurer analysisConfigurer, String customSettingsFile) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER, analysisConfigurer )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						customSettingsFile
				)
				.withIndex( index )
				.setup();

		operation.apply( index.schemaManager() ).join();
	}

	public static class EmptyElasticsearchAnalysisConfigurer implements ElasticsearchAnalysisConfigurer {

		@Override
		public void configure(ElasticsearchAnalysisConfigurationContext context) {
			// No-op
		}
	}

}
