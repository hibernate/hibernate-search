/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.categories.RequiresIndexOpenClose;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Tests related to index custom mapping when updating indexes.
 */
@Category(RequiresIndexOpenClose.class)
@TestForIssue(jiraKey = "HSEARCH-4253")
public class ElasticsearchIndexSchemaManagerUpdateCustomMappingIT {

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@Test
	public void noOverlapping() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
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
	public void illegalUpdate() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
				"    '_source':{ " +
				"       'enabled':true " +
				"    } " +
				" } "
		);

		assertThatThrownBy( () -> setupAndUpdateIndex( "no-overlapping.json" ) )
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

		Futures.unwrappedExceptionJoin( index.schemaManager().createOrUpdate() );
	}
}
