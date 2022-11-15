/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.common.reporting.FailureReportChecker;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Basic tests related to the mapping when validating indexes,
 * for all index-validating schema management operations.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaValidationIT")
class ElasticsearchIndexSchemaManagerValidationMappingBaseIT {

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
	void success_1(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "myField", f -> f.asLocalDate() )
					.toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'date',"
								+ "  'index': true,"
								+ "  'format': '"
								+ elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat() + "',"
								+ "  'ignore_malformed': true" // Ignored during validation
								+ "},"
								+ "'NOTmyField': {" // Ignored during validation
								+ "  'type': 'date',"
								+ "  'index': true"
								+ "}"
				)
		);

		setupAndValidate( index, operation );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_2(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "myField", f -> f.asBoolean() )
					.toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'boolean',"
								+ "  'index': true"
								+ "},"
								+ "'NOTmyField': {" // Ignored during validation
								+ "  'type': 'boolean',"
								+ "  'index': true"
								+ "}"
				)
		);

		setupAndValidate( index, operation );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success_3(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field(
					"myField",
					f -> f.asString().analyzer( "default" )
			)
					.toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'analyzer': 'default'"
								+ "},"
								+ "'NOTmyField': {" // Ignored during validation
								+ "  'type': 'text',"
								+ "  'index': true"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void attribute_field_notPresent(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable(
				root -> root.field( "myField", f -> f.asInteger() ).toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'notMyField': {"
								+ "    'type': 'integer',"
								+ "    'index': true"
								+ "  }"
				)
		);

		setupAndValidateExpectingFailure(
				index,
				hasValidationFailureReport()
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" ),
				operation
		);
	}

	/**
	 * Tests that mappings that are more powerful than requested will pass validation.
	 */
	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void property_attribute_leniency(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "myField", f -> f.asLong() )
					.toReference();
			root.field( "myTextField", f -> f.asString().analyzer( "default" ) )
					.toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "  'type': 'long',"
								+ "  'index': true,"
								+ "  'store': true"
								+ "},"
								+ "'myTextField': {"
								+ "  'type': 'text',"
								+ "  'index': true,"
								+ "  'norms': true"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void floatAndDouble_nullValue(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "float", f -> f.asFloat().indexNullAs( 1.7F ) ).toReference();
			root.field( "double", f -> f.asDouble().indexNullAs( 1.7 ) ).toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'float': {"
								+ "  'type': 'float',"
								+ "  'null_value': 1.7"
								+ "},"
								+ "'double': {"
								+ "  'type': 'double',"
								+ "  'null_value': 1.7"
								+ "}"
				)
		);

		setupAndValidate( index, operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void floatAndDouble_nullValue_invalids(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "float", f -> f.asFloat().indexNullAs( 1.7F ) ).toReference();
			root.field( "double", f -> f.asDouble().indexNullAs( 1.7 ) ).toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'float': {"
								+ "  'type': 'float',"
								+ "  'null_value': 1.9"
								+ "},"
								+ "'double': {"
								+ "  'type': 'double',"
								+ "  'null_value': 1.9"
								+ "}"
				)
		);

		setupAndValidateExpectingFailure( index,
				hasValidationFailureReport()
						.indexFieldContext( "double" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '1.7', actual is '1.9'" )
						.indexFieldContext( "float" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '1.7', actual is '1.9'" ),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void floatAndDouble_nullValue_invalids_notNumbers(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "float", f -> f
					.extension( ElasticsearchExtension.get() )
					.asNative().mapping( "{'type': 'float', 'null_value': AAA }" )
			).toReference();
			root.field( "double", f -> f
					.extension( ElasticsearchExtension.get() )
					.asNative().mapping( "{'type': 'double', 'null_value': BBB }" )
			).toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'float': {"
								+ "  'type': 'float',"
								+ "  'null_value': 1.9"
								+ "},"
								+ "'double': {"
								+ "  'type': 'double',"
								+ "  'null_value': 1.9"
								+ "}"
				)
		);

		setupAndValidateExpectingFailure( index,
				hasValidationFailureReport()
						.indexFieldContext( "double" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '\"BBB\"', actual is '1.9'" )
						.indexFieldContext( "float" )
						.mappingAttributeContext( "null_value" )
						.failure( "Invalid value. Expected '\"AAA\"', actual is '1.9'" ),
				operation
		);
	}

	/**
	 * Tests that properties within properties are correctly represented in the failure report.
	 */
	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void nestedProperty_attribute_invalid(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			IndexSchemaObjectField objectField =
					root.objectField( "myObjectField" );
			objectField.field( "myField", f -> f.asLocalDate() )
					.toReference();
			objectField.toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'myObjectField': {"
								+ "  'type': 'object',"
								+ "  'dynamic': 'strict',"
								+ "  'properties': {"
								+ "    'myField': {"
								+ "      'type': 'date',"
								+ "      'format': '"
								+ elasticSearchClient.getDialect().getLocalDateDefaultMappingFormat() + "',"
								+ "      'index': false"
								+ "    }"
								+ "  }"
								+ "}"
				)
		);

		setupAndValidateExpectingFailure(
				index,
				hasValidationFailureReport()
						.indexFieldContext( "myObjectField.myField" )
						.mappingAttributeContext( "index" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" ),
				operation
		);
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void multipleErrors(ElasticsearchIndexSchemaManagerValidationOperation operation) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.field( "myField", f -> f.asString() )
					.toReference();
		} );

		elasticSearchClient.index( index.name() ).deleteAndCreate();
		elasticSearchClient.index( index.name() ).type().putMapping(
				"{"
						+ "  'dynamic': false,"
						+ "  'properties': {"
						+ defaultMetadataMappingAndCommaForInitialization()
						+ "    'myField': {"
						+ "      'type': 'integer'"
						+ "    }"
						+ "  }"
						+ "}"
		);

		setupAndValidateExpectingFailure(
				index,
				hasValidationFailureReport()
						.mappingAttributeContext( "dynamic" )
						.failure(
								"Invalid value. Expected 'STRICT', actual is 'FALSE'"
						)
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "type" )
						.failure(
								"Invalid value. Expected 'keyword', actual is 'integer'"
						),
				operation
		);
	}

	private void setupAndValidateExpectingFailure(StubMappedIndex index, FailureReportChecker failureReportChecker,
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		assertThatThrownBy( () -> setupAndValidate( index, operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( failureReportChecker );
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
