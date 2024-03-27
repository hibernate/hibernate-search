/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.IndexFieldReference;
import org.hibernate.search.engine.backend.document.IndexObjectFieldReference;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.SimpleMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to index custom mapping when creating indexes,
 * for all index-creating schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-4253")
class ElasticsearchIndexSchemaManagerCreationCustomMappingIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.creating().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final SimpleMappedIndex<IndexBinding> index = SimpleMappedIndex.of( IndexBinding::new );

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void noOverlapping(ElasticsearchIndexSchemaManagerOperation operation) {
		setupAndCreateIndex( "no-overlapping.json", operation );
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
						"          'dynamic':'strict', " +
						"          'properties':{ " +
						"             'bothNested':{ " +
						"                'type':'keyword', " +
						"                'doc_values':false " +
						"             }, " +
						"             'bothNestedObject':{ " +
						"                'dynamic':'strict', " +
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
						"                'type':'object', " +
						"                'dynamic':'strict' " +
						"             } " +
						"          } " +
						"       }, " +
						"       'searchField':{ " +
						"          'type':'keyword', " +
						"          'doc_values':false " +
						"       }, " +
						"       'searchObject':{ " +
						"          'type':'object', " +
						"          'dynamic':'strict' " +
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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void complexConflicts(ElasticsearchIndexSchemaManagerOperation operation) {
		setupAndCreateIndex( "complex-conflicts.json", operation );
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
						"          'norms':true " +
						"       }, " +
						"       'bothObject':{ " +
						"          'dynamic':'strict', " +
						"          'properties':{ " +
						"             'bothNested':{ " +
						"                'type':'keyword', " +
						"                'norms':true " +
						"             }, " +
						"             'bothNestedObject':{ " +
						"                'dynamic':'true', " +
						"                'properties':{ " +
						"                   'bothNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'norms':true " +
						"                   }, " +
						"                   'searchNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'doc_values':false " +
						"                   }, " +
						"                   'userNestedNested':{ " +
						"                      'type':'keyword', " +
						"                      'norms':true " +
						"                   } " +
						"                } " +
						"             }, " +
						"             'searchNested':{ " +
						"                'type':'keyword', " +
						"                'doc_values':false " +
						"             }, " +
						"             'searchNestedObject':{ " +
						"                'type':'object', " +
						"                'dynamic':'strict' " +
						"             }, " +
						"             'userNested':{ " +
						"                'type':'keyword', " +
						"                'norms':true " +
						"             }, " +
						"             'userNestedObject':{ " +
						"                'type':'object', " +
						"                'dynamic':'true' " +
						"             } " +
						"          } " +
						"       }, " +
						"       'searchField':{ " +
						"          'type':'keyword', " +
						"          'doc_values':false " +
						"       }, " +
						"       'searchObject':{ " +
						"          'type':'object', " +
						"          'dynamic':'strict' " +
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

	private void setupAndCreateIndex(String customMappingFile, ElasticsearchIndexSchemaManagerOperation operation) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/" + customMappingFile
				)
				.withIndex( index )
				.setup();

		operation.apply( index.schemaManager() ).join();
	}

	private static class IndexBinding {
		final IndexFieldReference<String> searchField;
		final IndexFieldReference<String> bothField;

		final IndexObjectFieldReference searchObject;

		final IndexObjectFieldReference bothObject;
		final IndexFieldReference<String> searchNested;
		final IndexFieldReference<String> bothNested;

		final IndexObjectFieldReference searchNestedObject;

		final IndexObjectFieldReference bothNestedObject;
		final IndexFieldReference<String> searchNestedNested;
		final IndexFieldReference<String> bothNestedNested;

		IndexBinding(IndexSchemaElement root) {
			searchField = root.field( "searchField", f -> f.asString() ).toReference();
			bothField = root.field( "bothField", f -> f.asString() ).toReference();

			searchObject = root.objectField( "searchObject" ).toReference();

			IndexSchemaObjectField bObject = root.objectField( "bothObject" );
			bothObject = bObject.toReference();
			searchNested = bObject.field( "searchNested", f -> f.asString() ).toReference();
			bothNested = bObject.field( "bothNested", f -> f.asString() ).toReference();

			searchNestedObject = bObject.objectField( "searchNestedObject" ).toReference();

			IndexSchemaObjectField bNestedObject = bObject.objectField( "bothNestedObject" );
			bothNestedObject = bNestedObject.toReference();
			searchNestedNested = bNestedObject.field( "searchNestedNested", f -> f.asString() ).toReference();
			bothNestedNested = bNestedObject.field( "bothNestedNested", f -> f.asString() ).toReference();
		}
	}
}
