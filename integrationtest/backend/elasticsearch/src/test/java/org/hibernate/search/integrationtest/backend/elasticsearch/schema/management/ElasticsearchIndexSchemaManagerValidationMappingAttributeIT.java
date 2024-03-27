/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.Searchable;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to mapping attributes when validating indexes,
 * for all index-validating schema management operations.
 * <p>
 * These tests are more specific than {@link ElasticsearchIndexSchemaManagerValidationMappingBaseIT}
 * and focus on specific mapping attributes.
 */
class ElasticsearchIndexSchemaManagerValidationMappingAttributeIT {

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
	void attribute_dynamic_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'properties': {"
						+ defaultMetadataMappingAndCommaForInitialization()
						+ "   'myField': {"
						+ "     'type': 'integer',"
						+ "     'index': true"
						+ "   }"
						+ " }"
						+ "}"
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.mappingAttributeContext( "dynamic" )
						.failure( "Invalid value. Expected 'STRICT', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_dynamic_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic': false,"
						+ " 'properties': {"
						+ defaultMetadataMappingAndCommaForInitialization()
						+ "   'myField': {"
						+ "     'type': 'integer',"
						+ "     'index': true"
						+ "   }"
						+ " }"
						+ "}"
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.mappingAttributeContext( "dynamic" )
						.failure( "Invalid value. Expected 'STRICT', actual is 'FALSE'" ) );

	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_properties_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic': 'strict'"
						+ "}"
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "_entity_type" )
						.failure( "Missing property mapping" )
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_properties_empty(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ " 'dynamic': 'strict',"
						+ " 'properties': {"
						+ " }"
						+ "}"
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "_entity_type" )
						.failure( "Missing property mapping" )
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_type_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'keyword',"
								+ "  'index': true"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "type" )
						.failure( "Invalid value. Expected 'integer', actual is 'keyword'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_index_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer'"
								+ "}"
				)
		);

		// the expected value true is the default
		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_index_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().searchable( Searchable.YES ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'index': true"
								+ "}"
				)
		);

		// the expected value true is the default
		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_index_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'index': false"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "index" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_index_false_scalar(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				// Searchable.NO allows "index" being set to false
				root -> root.field( "myField", f -> f.asInteger().searchable( Searchable.NO ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'index': false"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_index_false_text(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				// Searchable.NO allows "index" being set to false
				root -> root.field( "myField", f -> f.asString().analyzer( "keyword" ).searchable( Searchable.NO ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'analyzer': 'keyword',"
								+ "  'index': false"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_format_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asLocalDate() ).toReference()
		);

		String allFormats = elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat();

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'date'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure( "The output format (the first element) is invalid." )
						.failure(
								"Invalid formats",
								"missing elements are '[" + allFormats + "]'"
						) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_format_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asLocalDate() ).toReference()
		);

		String allFormats = elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat();

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'date',"
								+ "  'format': '" + allFormats + "'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_format_exceeding(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asLocalDate() ).toReference()
		);

		String allFormats = elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat();

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'date',"
								+ "  'format': '" + allFormats + "||yyyy" + "'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure(
								"Invalid formats",
								"unexpected elements are '[yyyy]'"
						) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_format_wrong(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asLocalDate() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'date',"
								+ "  'format': 'epoch_millis||strict_date_time'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "format" )
						.failure(
								"The output format (the first element) is invalid. Expected '"
										+ elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat()
										+ "', actual is 'epoch_millis'"
						) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_analyzer_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "keyword" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "analyzer" )
						.failure( "Invalid value. Expected 'keyword', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_analyzer_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "keyword" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_analyzer_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "default" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "analyzer" )
						.failure( "Invalid value. Expected 'default', actual is 'keyword'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_searchAnalyzer_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString()
						.analyzer( "keyword" ).searchAnalyzer( "italian" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "search_analyzer" )
						.failure( "Invalid value. Expected 'italian', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_searchAnalyzer_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString()
						.analyzer( "keyword" ).searchAnalyzer( "english" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword',"
								+ "  'search_analyzer': 'english'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_searchAnalyzer_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "keyword" ).searchAnalyzer( "italian" ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword',"
								+ "  'search_analyzer': 'english'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "search_analyzer" )
						.failure( "Invalid value. Expected 'italian', actual is 'english'" ) );
	}


	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4652")
	void attribute_searchAnalyzer_sameAsAnalyzer_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString()
						.analyzer( "keyword" ).searchAnalyzer( "keyword" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword',"
								+ "  'search_analyzer': 'keyword'"
								+ "  }"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	@TestForIssue(jiraKey = "HSEARCH-4652")
	void attribute_searchAnalyzer_sameAsAnalyzer_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "keyword" ).searchAnalyzer( "keyword" ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'keyword',"
								+ "  'search_analyzer': 'english'"
								+ "  }"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "search_analyzer" )
						.failure( "Invalid value. Expected 'keyword', actual is 'english'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_norms_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.NO ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'norms': false"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_norms_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.YES ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'norms': false"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "norms" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
				);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_norms_missing_textField(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "default" ).norms( Norms.YES ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_norms_missing_keywordField(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().norms( Norms.NO ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'keyword'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_termVector_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root
						.field( "myField",
								f -> f.asString().analyzer( "english" ).termVector( TermVector.WITH_POSITIONS_OFFSETS ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'analyzer': 'english',"
								+ "  'term_vector': 'with_positions_offsets'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_termVector_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "english" ).termVector( TermVector.NO ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'analyzer': 'english'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_termVector_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().analyzer( "english" ).termVector( TermVector.YES ) )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'analyzer': 'english',"
								+ "  'term_vector': 'with_offsets'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "term_vector" )
						.failure( "Invalid value. Expected 'yes', actual is 'with_offsets'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_nullValue_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'null_value': 739"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_nullValue_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '739', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_nullValue_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().indexNullAs( 739 ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'null_value': 777"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '739', actual is '777'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_docValues_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'doc_values': true"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_docValues_default(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_docValues_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger().sortable( Sortable.YES ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'doc_values': false"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "doc_values" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_docValues_false(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				// Sortable.NO and Sortable.DEFAULT allow doc_values being set to false
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'integer',"
								+ "  'doc_values': false"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_docValues_skip(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				// Sortable.NO and Sortable.DEFAULT do not require doc_values being set to false
				root -> root.field( "myField", f -> f.asString().sortable( Sortable.NO ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'keyword',"
								+ "  'doc_values': true"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_scaling_factor_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asBigDecimal().decimalScale( 2 ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'scaled_float',"
								+ "  'scaling_factor': 100"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_scaling_factor_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asBigDecimal().decimalScale( 2 ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'scaled_float',"
								+ "  'scaling_factor': 2"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "scaling_factor" )
						.failure( "Invalid value. Expected '100.0', actual is '2.0'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_normalizer_missing(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().normalizer( "default" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'keyword',"
								+ "  'index': true"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "normalizer" )
						.failure( "Invalid value. Expected 'default', actual is 'null'" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_normalizer_valid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().normalizer( "custom-normalizer" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
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
						+ "}"
		);
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'keyword',"
								+ "  'index': true,"
								+ "  'normalizer': 'custom-normalizer'"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_normalizer_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asString().normalizer( "another-normalizer" ) ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis",
				"{"
						+ " 'normalizer': {"
						+ "   'custom-normalizer': {"
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
						+ " }"
		);
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'keyword',"
								+ "  'index': true,"
								+ "  'normalizer': 'custom-normalizer'"
								+ "}"
				)
		);

		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( Exception.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "normalizer" )
						.failure( "Invalid value. Expected 'another-normalizer', actual is 'custom-normalizer'" ) );
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
