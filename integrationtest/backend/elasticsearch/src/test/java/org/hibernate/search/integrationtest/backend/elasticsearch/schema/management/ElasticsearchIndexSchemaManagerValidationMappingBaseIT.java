/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Basic tests related to the mapping when validating indexes,
 * for all index-validating schema management operations.
 */
@RunWith(Parameterized.class)
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaValidationIT")
public class ElasticsearchIndexSchemaManagerValidationMappingBaseIT {

	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";

	private static final String INDEX_NAME = "IndexName";

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> operations() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all();
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private final ElasticsearchIndexSchemaManagerValidationOperation operation;

	private StubMappingIndexManager indexManager;

	public ElasticsearchIndexSchemaManagerValidationMappingBaseIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success_1() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "'type': 'date',"
								+ "'index': true,"
								+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
								+ "'ignore_malformed': true" // Ignored during validation
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'date',"
								+ "'index': true"
						+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() )
					.toReference();
		} );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void success_2() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "'type': 'boolean',"
								+ "'index': true"
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'boolean',"
								+ "'index': true"
						+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asBoolean() )
					.toReference();
		} );

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void success_3() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "'type': 'text',"
								+ "'index': true,"
								+ "'analyzer': 'default'"
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'text',"
								+ "'index': true"
						+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field(
					"myField",
					f -> f.asString().analyzer( "default" )
			)
					.toReference();
		} );
	}

	@Test
	public void mapping_missing() throws Exception {
		Assume.assumeTrue(
				"Skipping this test as there is always a mapping (be it empty) in " + elasticSearchClient.getDialect(),
				elasticSearchClient.getDialect().isEmptyMappingPossible()
		);
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();

		setupAndValidateExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "myField", f -> f.asLocalDate() )
							.toReference();
				},
				FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.failure( "Missing type mapping" )
						.build()
		);
	}

	@Test
	public void attribute_field_notPresent() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'notMyField': {"
									+ "'type': 'integer',"
									+ "'index': true"
							+ "}"
				)
		);

		setupAndValidateExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "myField", f -> f.asInteger() ).toReference();
				},
				FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField" )
						.failure( "Missing property mapping" )
						.build()
		);
	}

	/**
	 * Tests that mappings that are more powerful than requested will pass validation.
	 */
	@Test
	public void property_attribute_leniency() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "'type': 'long',"
								+ "'index': true,"
								+ "'store': true"
						+ "},"
						+ "'myTextField': {"
								+ "'type': 'text',"
								+ "'index': true,"
								+ "'norms': true"
						+ "}"
				)
		);

		setupAndValidate( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLong() )
					.toReference();
			root.field( "myTextField", f -> f.asString().analyzer( "default" ) )
					.toReference();
		} );
	}

	/**
	 * Tests that properties within properties are correctly represented in the failure report.
	 */
	@Test
	public void nestedProperty_attribute_invalid() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'myObjectField': {"
								+ "'type': 'object',"
								+ "'dynamic': 'strict',"
								+ "'properties': {"
										+ "'myField': {"
												+ "'type': 'date',"
												+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
												+ "'index': false"
										+ "}"
								+ "}"
						+ "}"
				)
		);

		setupAndValidateExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					IndexSchemaObjectField objectField =
							root.objectField( "myObjectField" );
					objectField.field( "myField", f -> f.asLocalDate() )
							.toReference();
					objectField.toReference();
				},
				FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myObjectField.myField" )
						.mappingAttributeContext( "index" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
						.build()
		);
	}

	@Test
	public void multipleErrors() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
						+ "'dynamic': false,"
						+ "'properties': {"
								+ defaultMetadataMappingAndCommaForInitialization()
								+ "'myField': {"
										+ "'type': 'integer'"
								+ "}"
						+ "}"
				+ "}"
		);

		setupAndValidateExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "myField", f -> f.asString() )
							.toReference();
				},
				FailureReportUtils.buildFailureReportPattern()
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.mappingAttributeContext( "dynamic" )
						.failure(
								"Invalid value. Expected 'STRICT', actual is 'FALSE'"
						)
						.indexFieldContext( "myField" )
						.mappingAttributeContext( "type" )
						.failure(
								"Invalid value. Expected 'keyword', actual is 'integer'"
						)
						.build()
		);
	}

	private void setupAndValidateExpectingFailure(Consumer<? super IndexedEntityBindingContext> mappingContributor,
			String failureReportRegex) {
		SubTest.expectException( () -> setupAndValidate( mappingContributor ) )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( failureReportRegex );
	}

	private void setupAndValidate(Consumer<? super IndexedEntityBindingContext> mappingContributor) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndex( INDEX_NAME, mappingContributor, indexManager -> this.indexManager = indexManager )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( indexManager.getSchemaManager() ) );
	}
}
