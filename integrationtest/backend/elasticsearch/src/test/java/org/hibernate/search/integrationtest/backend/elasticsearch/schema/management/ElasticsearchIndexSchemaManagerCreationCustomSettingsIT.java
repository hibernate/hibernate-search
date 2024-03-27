/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
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
 * Tests related to index custom settings when creating indexes,
 * for all index-creating schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-3934")
class ElasticsearchIndexSchemaManagerCreationCustomSettingsIT {

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
	void success_mergeWithNoOverlapping(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// merge the default analysis configurer with the custom settings,
		// there are no overlapping of their definitions
		setupAndCreateIndex( null, "custom-index-settings/valid.json", operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_mergeWithOverlapping(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// merge the default analysis configurer with the custom settings,
		// there are some overlapping of their definitions
		setupAndCreateIndex( null, "custom-index-settings/overlapping.json", operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_onlyCustomSettings(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// use an empty analysis configurer,
		// so that we have only the custom settings definitions
		setupAndCreateIndex( new EmptyElasticsearchAnalysisConfigurer(), "custom-index-settings/valid.json", operation );

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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void maxResultWindow(ElasticsearchIndexSchemaManagerOperation operation) {
		elasticsearchClient.index( index.name() )
				.ensureDoesNotExist();

		// use an empty analysis configurer,
		// so that we have only the custom settings definitions
		setupAndCreateIndex( new EmptyElasticsearchAnalysisConfigurer(), "custom-index-settings/max-result-window.json",
				operation
		);

		assertJsonEquals(
				"\"250\"",
				elasticsearchClient.index( index.name() ).settings( "index.max_result_window" ).get()
		);
	}

	private void setupAndCreateIndex(ElasticsearchAnalysisConfigurer analysisConfigurer, String customSettingsFile,
			ElasticsearchIndexSchemaManagerOperation operation) {
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
