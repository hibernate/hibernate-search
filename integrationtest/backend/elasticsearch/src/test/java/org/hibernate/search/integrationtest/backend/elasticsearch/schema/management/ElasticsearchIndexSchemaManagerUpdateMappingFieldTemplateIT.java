/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEqualsIgnoringUnknownFields;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Tests related to the mapping when updating indexes and field templates are defined.
 */
class ElasticsearchIndexSchemaManagerUpdateMappingFieldTemplateIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	@Test
	void nothingToDo() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() )
					.matchingPathGlob( "*_t1" );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) )
					.matchingPathGlob( "*_t2" );
			root.fieldTemplate( "myTemplate3", f -> f.asString() )
					.matchingPathGlob( "*_t3" );
			root.objectFieldTemplate( "myTemplate4", ObjectStructure.NESTED );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*_t1',"
						+ "     'mapping': { 'type': 'integer', 'doc_values': false }"
						+ "   } },"
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*_t2',"
						+ "     'mapping': { 'type': 'text', 'analyzer': 'default' }"
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*_t3',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } },"
						+ "   { 'myTemplate4': {"
						+ "     'match_mapping_type': 'object',"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'nested' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*_t1',"
						+ "     'mapping':" + integerMappingForExpectations()
						+ "   } },"
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*_t2',"
						+ "     'mapping':" + textMappingForExpectations()
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*_t3',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } },"
						+ "   { 'myTemplate4': {"
						+ "     'match_mapping_type': 'object',"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'nested' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void missing() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) );
			root.fieldTemplate( "myTemplate3", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'integer', 'doc_values': false }"
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + integerMappingForExpectations()
						+ "   } },"
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + textMappingForExpectations()
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void extra() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) );
			root.fieldTemplate( "myTemplate3", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'integer', 'doc_values': false }"
						+ "   } },"
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'text', 'analyzer': 'default' }"
						+ "   } },"
						+ "   { 'extraTemplate': {"
						+ "     'path_match': '*_extra',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + integerMappingForExpectations()
						+ "   } },"
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + textMappingForExpectations()
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void wrongOrder() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) );
			root.fieldTemplate( "myTemplate3", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'text', 'analyzer': 'default' }"
						+ "   } },"
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'integer', 'doc_values': false }"
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + integerMappingForExpectations()
						+ "   } },"
						+ "   {'myTemplate2': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + textMappingForExpectations()
						+ "   } },"
						+ "   { 'myTemplate3': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void duplicate() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'integer', 'doc_values': false }"
						+ "   } },"
						+ "   { 'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate1': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + integerMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_pathMatch_missing() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_suffix" );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'match_mapping_type': 'object',"
						+ "     'mapping': { 'type': 'nested' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEquals(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'match_mapping_type': 'object',"
						+ "     'path_match': '*_suffix',"
						+ "     'mapping': { 'type': 'nested', 'dynamic': 'true' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_pathMatch_invalid() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectStructure.NESTED )
					.matchingPathGlob( "*_suffix" );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'match_mapping_type': 'object',"
						+ "     'path_match': '*_suffix2',"
						+ "     'mapping': { 'type': 'nested' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEquals(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'match_mapping_type': 'object',"
						+ "     'path_match': '*_suffix',"
						+ "     'mapping': { 'type': 'nested', 'dynamic': 'true' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_pathMatch_extra() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*_suffix',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_matchMappingType_missing() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectStructure.NESTED );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'nested' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEquals(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'match_mapping_type': 'object',"
						+ "     'mapping': { 'type': 'nested', 'dynamic': 'true' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_matchMappingType_invalid() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectStructure.NESTED );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'match_mapping_type': 'long',"
						+ "     'mapping': { 'type': 'nested' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEquals(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'match_mapping_type': 'object',"
						+ "     'mapping': { 'type': 'nested', 'dynamic': 'true' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_matchMappingType_extra() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'match_mapping_type': 'long',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void attribute_extra() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'match': '*_suffix',"
						+ "     'mapping': { 'type': 'keyword' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	void mapping_invalid() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'mapping': { 'type': 'integer' }"
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForInitialization()
						+ " }"
						+ "}"
		);

		setupAndUpdate( index );

		assertJsonEqualsIgnoringUnknownFields(
				"{"
						+ " 'dynamic': 'true',"
						+ " 'dynamic_templates': ["
						+ "   {'myTemplate': {"
						+ "     'path_match': '*',"
						+ "     'mapping':" + keywordMappingForExpectations()
						+ "   } }"
						+ " ],"
						+ " 'properties': {"
						+ defaultMetadataMappingForExpectations()
						+ " }"
						+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	private void setupAndUpdate(StubMappedIndex index) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( index.schemaManager().createOrUpdate( OperationSubmitter.blocking() ) );
	}

	private String integerMappingForExpectations() {
		return "{"
				+ "  'type': 'integer',"
				+ "  'doc_values': false"
				+ "}";
	}

	private String textMappingForExpectations() {
		return "{"
				+ "  'type': 'text',"
				+ "  'analyzer': 'default'"
				+ "}";
	}

	private String keywordMappingForExpectations() {
		return "{"
				+ "  'type': 'keyword'"
				+ "}";
	}
}
