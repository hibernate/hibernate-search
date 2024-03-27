/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
 * Tests related to index custom mapping when updating indexes.
 */
@TestForIssue(jiraKey = "HSEARCH-4253")
class ElasticsearchIndexSchemaManagerUpdateCustomMappingIT {

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
	void noOverlapping() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
						"    'dynamic':'strict', " +
						"    '_source':{ " +
						"       'enabled':false " +
						"    }, " +
						"    'properties':{ " +
						"       '_entity_type':{ " +
						"          'type':'keyword', " +
						"          'index':false " +
						"       }, " +
						"       'bothField':{ " +
						"          'type':'keyword', " +
						"          'doc_values':false " +
						"       }, " +
						"       'bothObject':{ " +
						"          'properties':{ " +
						"             'bothNested':{ " +
						"                'type':'keyword', " +
						"                'doc_values':false " +
						"             }, " +
						"             'bothNestedObject':{ " +
						"                'properties':{ " +
						"                   'bothNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'doc_values':false " +
						"                   }, " +
						"                   'searchNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'doc_values':false " +
						"                   } " +
						"                } " +
						"             }, " +
						"             'searchNested':{ " +
						"                'type':'keyword', " +
						"                'doc_values':false " +
						"             }, " +
						"             'searchNestedObject':{ " +
						"                'type':'object' " +
						"             } " +
						"          } " +
						"       }, " +
						"       'searchField':{ " +
						"          'type':'keyword', " +
						"          'doc_values':false " +
						"       }, " +
						"       'searchObject':{ " +
						"          'type':'object' " +
						"       } " +
						"    } " +
						" } "
		);

		setupAndUpdateIndex( "no-overlapping.json" );
		assertJsonEquals(
				" { " +
						"    'dynamic':'strict', " +
						"    '_source':{ " +
						"       'enabled':false " +
						"    }, " +
						"    'properties':{ " +
						"       '_entity_type':{ " +
						"          'type':'keyword', " +
						"          'index':false " +
						"       }, " +
						"       'bothField':{ " +
						"          'type':'keyword', " +
						"          'doc_values':false " +
						"       }, " +
						"       'bothObject':{ " +
						"          'properties':{ " +
						"             'bothNested':{ " +
						"                'type':'keyword', " +
						"                'doc_values':false " +
						"             }, " +
						"             'bothNestedObject':{ " +
						"                'properties':{ " +
						"                   'bothNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'doc_values':false " +
						"                   }, " +
						"                   'searchNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'doc_values':false " +
						"                   } " +
						"                } " +
						"             }, " +
						"             'searchNested':{ " +
						"                'type':'keyword', " +
						"                'doc_values':false " +
						"             }, " +
						"             'searchNestedObject':{ " +
						"                'type':'object' " +
						"             } " +
						"          } " +
						"       }, " +
						"       'searchField':{ " +
						"          'type':'keyword', " +
						"          'doc_values':false " +
						"       }, " +
						"       'searchObject':{ " +
						"          'type':'object' " +
						"       }, " +
						"       'userField':{ " +
						"          'type':'keyword', " +
						"          'norms':true " +
						"       }, " +
						"       'userObject':{ " +
						"          'type':'object', " +
						"          'dynamic':'true' " +
						"       } " +
						"    } " +
						" } ",
				elasticsearchClient.index( index.name() ).type().getMapping() );
	}

	@Test
	void illegalUpdate() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
						"    '_source':{ " +
						"       'enabled': false " +
						"    } " +
						" } "
		);

		assertThatThrownBy( () -> setupAndUpdateIndex( "source-enabled.json" ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( "conflicts", "_source" );
	}

	private void setupAndUpdateIndex(String customMappingFile) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/" + customMappingFile
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( index.schemaManager().createOrUpdate( OperationSubmitter.blocking() ) );
	}
}
