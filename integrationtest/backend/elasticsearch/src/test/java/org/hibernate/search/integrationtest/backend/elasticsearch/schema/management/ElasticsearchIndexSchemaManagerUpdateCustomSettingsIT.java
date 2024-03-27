/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests related to index custom settings when updating indexes.
 */
@TestForIssue(jiraKey = "HSEARCH-3934")
class ElasticsearchIndexSchemaManagerUpdateCustomSettingsIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@BeforeEach
	void checkAssumption() {
		assumeTrue(
				ElasticsearchTckBackendFeatures.supportsIndexClosingAndOpening(),
				"This test only is only relevant if we are allowed to open/close Elasticsearch indexes."
		);
	}

	@Test
	void nothingToDo() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index",
				" { " +
						"   'number_of_shards': '3', " +
						"   'number_of_replicas': '3', " +
						"   'analysis': { " +
						"     'analyzer': { " +
						"       'my_standard-english': { " +
						"         'type': 'standard', " +
						"         'stopwords': '_english_' " +
						"       }, " +
						"       'my_analyzer_ngram': { " +
						"         'type': 'custom', " +
						"         'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"       } " +
						"     }, " +
						"     'tokenizer': { " +
						"       'my_analyzer_ngram_tokenizer': { " +
						"         'type': 'ngram', " +
						"         'min_gram': '5', " +
						"         'max_gram': '6' " +
						"       } " +
						"     } " +
						"   } " +
						" } "
		);

		setupAndUpdateIndex();

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
	void change_analysis() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index",
				" { " +
						"   'number_of_shards': '3', " +
						"   'number_of_replicas': '3', " +
						"   'analysis': { " +
						"     'analyzer': { " +
						"       'my_standard-english': { " +
						"         'type': 'standard', " +
						"         'stopwords': '_english_' " +
						"       }, " +
						"       'my_analyzer_ngram': { " +
						"         'type': 'custom', " +
						"         'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"       } " +
						"     }, " +
						"     'tokenizer': { " +
						"       'my_analyzer_ngram_tokenizer': { " +
						"         'type': 'ngram', " +
						"         'min_gram': '2', " +
						"         'max_gram': '3' " +
						"       } " +
						"     } " +
						"   } " +
						" } "
		);

		setupAndUpdateIndex();

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
	void change_numberOfShards() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index",
				" { " +
						"   'number_of_shards': '7', " +
						"   'number_of_replicas': '3', " +
						"   'analysis': { " +
						"     'analyzer': { " +
						"       'my_standard-english': { " +
						"         'type': 'standard', " +
						"         'stopwords': '_english_' " +
						"       }, " +
						"       'my_analyzer_ngram': { " +
						"         'type': 'custom', " +
						"         'tokenizer': 'my_analyzer_ngram_tokenizer' " +
						"       } " +
						"     }, " +
						"     'tokenizer': { " +
						"       'my_analyzer_ngram_tokenizer': { " +
						"         'type': 'ngram', " +
						"         'min_gram': '2', " +
						"         'max_gram': '3' " +
						"       } " +
						"     } " +
						"   } " +
						" } "
		);

		assertThatThrownBy( () -> setupAndUpdateIndex() )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll(
						"Unable to update settings", "index.number_of_shards" );
	}

	@Test
	void change_maxResultWindow() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ 'max_result_window': '20000' }" );

		setupAndUpdateIndex( "max-result-window.json" );

		assertJsonEquals(
				"\"250\"",
				elasticsearchClient.index( index.name() ).settings( "index.max_result_window" ).get()
		);
	}

	@Test
	void set_maxResultWindow() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ }" );

		setupAndUpdateIndex( "max-result-window.json" );

		assertJsonEquals(
				"\"250\"",
				elasticsearchClient.index( index.name() ).settings( "index.max_result_window" ).get()
		);
	}

	private void setupAndUpdateIndex() {
		setupAndUpdateIndex( "valid.json" );
	}

	private void setupAndUpdateIndex(String customSettingsFile) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// use an empty analysis configurer,
						// so that we have only the custom settings definitions
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_SETTINGS_FILE,
						"custom-index-settings/" + customSettingsFile
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( index.schemaManager().createOrUpdate( OperationSubmitter.blocking() ) );
	}

}
