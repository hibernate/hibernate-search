/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.management.ElasticsearchManagementTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.management.ElasticsearchManagementTestUtils.simpleMappingForInitialization;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for the schema validation feature when using automatic index management.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaValidationIT")
public class ElasticsearchSchemaValidationIT {

	private static final String SCHEMA_VALIDATION_CONTEXT = "schema validation";
	private static final String FACET_FIELD_SUFFIX = "__HSearch_Facet";

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX1_NAME = "Index1Name";
	private static final String INDEX2_NAME = "Index2Name";
	private static final String INDEX3_NAME = "Index3Name";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
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
		elasticSearchClient.index( INDEX2_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX2_NAME ).type().putMapping(
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
		elasticSearchClient.index( INDEX3_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX3_NAME ).type().putMapping(
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

		/*
		 TODO uncomment and adapt this once we restore support for faceting; see HSEARCH-3271

		elasticSearchClient.index( INDEX4_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX4_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'myField': {"
								+ "'type': 'date',"
								+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
								+ "'ignore_malformed': true," // Ignored during validation
								+ "'fields': {"
										+ "'myField" + FACET_FIELD_SUFFIX + "': {"
												+ "'type': 'date'"
										+ "}"
								+ "}"
						+ "},"
						+ "'NOTmyField': {" // Ignored during validation
								+ "'type': 'date',"
								+ "'index': 'false'"
						+ "}"
				)
		);
		 */

		validateSchemaConfig()
				.withIndex(
						INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() )
									.toReference();
						}
				)
				.withIndex(
						INDEX2_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean() )
									.toReference();
						}
				)
				.withIndex(
						INDEX3_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"myField",
									f -> f.asString().analyzer( "default" )
							)
									.toReference();
						}
				)
				.setup();
		// TODO add an index with a faceted field once we restore support for faceting; see HSEARCH-3271

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void mapping_missing() throws Exception {
		Assume.assumeTrue(
				"Skipping this test as there is always a mapping (be it empty) in " + elasticSearchClient.getDialect(),
				elasticSearchClient.getDialect().isEmptyMappingPossible()
		);
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();

		setupExpectingFailure(
				() -> setupSimpleIndexWithLocalDateField( INDEX1_NAME ),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.failure( "Missing type mapping" )
						.build()
		);
	}

	@Test
	public void attribute_field_notPresent() {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'notMyField': {"
									+ "'type': 'integer',"
									+ "'index': true"
							+ "}"
				)
		);

		setupExpectingFailure(
				() -> validateSchemaConfig()
						.withIndex(
								INDEX1_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asInteger() ).toReference();
								}
						)
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
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
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
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

		validateSchemaConfig()
				.withIndex(
						INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLong() )
									.toReference();
							root.field( "myTextField", f -> f.asString().analyzer( "default" ) )
									.toReference();
						}
				)
				.setup();
	}

	/**
	 * Tests that properties within properties are correctly represented in the failure report.
	 */
	@Test
	public void nestedProperty_attribute_invalid() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
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

		setupExpectingFailure(
				() -> validateSchemaConfig()
						.withIndex(
								INDEX1_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									IndexSchemaObjectField objectField =
											root.objectField( "myObjectField" );
									objectField.field( "myField", f -> f.asLocalDate() )
											.toReference();
									objectField.toReference();
								}
						)
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myObjectField.myField" )
						.mappingAttributeContext( "index" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
						.build()
		);
	}

	@Test
	public void multipleErrors_singleIndexManagers() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
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

		setupExpectingFailure(
				() -> setupSimpleIndexWithKeywordField( INDEX1_NAME ),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
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

	@Test
	public void multipleErrors_multipleIndexManagers() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
						+ "'dynamic': false,"
						+ "'properties': {"
								+ defaultMetadataMappingAndCommaForInitialization()
								+ "'myField': {"
										+ "'type': 'keyword'"
								+ "}"
						+ "}"
				+ "}"
		);
		elasticSearchClient.index( INDEX2_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX2_NAME ).type().putMapping(
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

		setupExpectingFailure(
				() -> validateSchemaConfig()
						.withIndex(
								INDEX1_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asString() )
											.toReference();
								}
						)
						.withIndex(
								INDEX2_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asString() )
											.toReference();
								}
						)
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.mappingAttributeContext( "dynamic" )
						.failure(
								"Invalid value. Expected 'STRICT', actual is 'FALSE'"
						)
						.indexContext( INDEX2_NAME )
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

	@Test
	public void property_fields_missing() throws Exception {
		Assume.assumeTrue( "Faceting/sub-fields are not supported yet; see HSEARCH-3465, HSEARCH-3271", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'id': {"
								+ "'type': 'keyword',"
								+ "'store': true"
						+ "},"
						+ "'myField': {"
								+ "'type': 'date'"
						+ "}"
				)
		);

		setupExpectingFailure(
				() -> validateSchemaConfig()
						// TODO add an index with a faceted field or sub-field when APIs allow it; see HSEARCH-3465, HSEARCH-3271
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField.myField" + FACET_FIELD_SUFFIX )
						.failure(
								"Missing field mapping"
						)
						.build()
		);
	}

	@Test
	public void property_field_missing() throws Exception {
		Assume.assumeTrue( "Faceting/sub-fields are not supported yet; see HSEARCH-3465, HSEARCH-3271", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'id': {"
								+ "'type': 'keyword',"
								+ "'store': true"
						+ "},"
						+ "'myField': {"
								+ "'type': 'date',"
								+ "'fields': {"
										+ "'NOTmyField" + FACET_FIELD_SUFFIX + "': {"
												+ "'type': 'date'"
										+ "}"
								+ "}"
						+ "}"
				)
		);

		setupExpectingFailure(
				() -> validateSchemaConfig()
						// TODO add an index with a faceted field or sub-field when APIs allow it; see HSEARCH-3465, HSEARCH-3271
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField.myField" + FACET_FIELD_SUFFIX )
						.failure(
								"Missing field mapping"
						)
						.build()
		);
	}

	@Test
	public void property_field_attribute_invalid() throws Exception {
		Assume.assumeTrue( "Faceting/sub-fields are not supported yet; see HSEARCH-3465, HSEARCH-3271", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'id': {"
								+ "'type': 'keyword',"
								+ "'index': 'true',"
								+ "'store': true"
						+ "},"
						+ "'myField': {"
								+ "'type': 'date',"
								+ "'index': 'false',"
								+ "'fields': {"
										+ "'myField" + FACET_FIELD_SUFFIX + "': {"
												+ "'type': 'date',"
												+ "'index': 'false'"
										+ "}"
								+ "}"
						+ "}"
				)
				);

		setupExpectingFailure(
				() -> validateSchemaConfig()
						// TODO add an index with a faceted field or sub-field when APIs allow it; see HSEARCH-3465, HSEARCH-3271
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.contextLiteral( SCHEMA_VALIDATION_CONTEXT )
						.indexFieldContext( "myField.myField" + FACET_FIELD_SUFFIX )
						.failure(
								"Invalid value for attribute 'index'. Expected 'true', actual is 'false'"
						)
						.build()
		);
	}

	private void setupExpectingFailure(Runnable setupAction, String failureReportRegex) {
		SubTest.expectException( setupAction )
				.assertThrown()
				.isInstanceOf( SearchException.class )
				.hasMessageMatching( failureReportRegex );
	}

	private void setupSimpleIndexWithKeywordField(String indexName) {
		validateSchemaConfig()
				.withIndex(
						indexName,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString() )
									.toReference();
						}
				)
				.setup();
	}

	private void setupSimpleIndexWithLocalDateField(String indexName) {
		validateSchemaConfig()
				.withIndex(
						indexName,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() )
									.toReference();
						}
				)
				.setup();
	}

	private SearchSetupHelper.SetupContext validateSchemaConfig() {
		return setupHelper.start( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.VALIDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				);
	}
}
