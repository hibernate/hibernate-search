/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to field templates when validating indexes,
 * for all index-validating schema management operations.
 * <p>
 * These tests are more specific than {@link ElasticsearchIndexSchemaManagerValidationMappingBaseIT}
 * and focus on field templates.
 */
class ElasticsearchIndexSchemaManagerValidationMappingFieldTemplateIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate2" )
						.failure( "Missing dynamic field template" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void extra(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "extraTemplate" )
						.failure( "Unexpected dynamic field template" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void wrongOrder(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.failure(
								"Invalid order for dynamic field templates",
								"Expected [myTemplate1, myTemplate2, myTemplate3]",
								"actual is [myTemplate2, myTemplate1, myTemplate3]"
						) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void duplicate(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate1" )
						.failure(
								"Multiple dynamic field templates with this name",
								"The names of dynamic field template must be unique"
						) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_pathMatch_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "path_match" )
						.failure( "Invalid value. Expected '*_suffix', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_pathMatch_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "path_match" )
						.failure( "Invalid value. Expected '*_suffix', actual is '*_suffix2'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_pathMatch_extra(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "path_match" )
						.failure( "Invalid value. Expected '*', actual is '*_suffix'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_matchMappingType_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "match_mapping_type" )
						.failure( "Invalid value. Expected 'object', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_matchMappingType_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "match_mapping_type" )
						.failure( "Invalid value. Expected 'object', actual is 'long'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_matchMappingType_extra(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "match_mapping_type" )
						.failure( "Invalid value. Expected 'null', actual is 'long'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_extra(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "match" )
						.failure( "Invalid value. Expected 'null', actual is '\"*_suffix\"'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void mapping_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldTemplateContext( "myTemplate" )
						.fieldTemplateAttributeContext( "mapping" )
						.mappingAttributeContext( "type" )
						.failure( "Invalid value. Expected 'keyword', actual is 'integer'" ) );
	}

	private void setupAndValidate(StubMappedIndex index, ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}
}
