/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.junit.Assume.assumeFalse;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchTestHostConnectionConfiguration;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to index custom settings when updating indexes.
 */
@TestForIssue(jiraKey = "HSEARCH-3934")
public class ElasticsearchIndexSchemaManagerUpdateCustomSettingsIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Before
	public void checkAssumption() {
		assumeFalse(
				"This test only is only relevant if we are allowed to open/close Elasticsearch indexes." +
						" These operations are not available on AWS in particular.",
				ElasticsearchTestHostConnectionConfiguration.get().isAws()
		);
	}

	@Test
	public void nothingToDo() {
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
	public void change_analysis() {
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
	public void change_numberOfShards() {
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
	public void change_maxResultWindow() {
		elasticsearchClient.index( index.name() ).deleteAndCreate( "index", "{ 'max_result_window': '20000' }" );

		setupAndUpdateIndex( "max-result-window.json" );

		assertJsonEquals(
				"\"250\"",
				elasticsearchClient.index( index.name() ).settings( "index.max_result_window" ).get()
		);
	}

	@Test
	public void set_maxResultWindow() {
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
