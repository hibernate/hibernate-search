/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.util.impl.test.ExceptionMatcherBuilder.isException;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchIndexNameNormalizer;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for the schema validation feature when using automatic index management.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaValidationIT")
public class ElasticsearchSchemaValidationIT {

	private static final String VALIDATION_FAILED_MESSAGE_ID = "HSEARCH400033";
	private static final String FACET_FIELD_SUFFIX = "__HSearch_Facet";

	private static final String BACKEND_NAME = "myElasticsearchBackend";
	private static final String INDEX1_NAME = "Index1Name";
	private static final String INDEX2_NAME = "Index2Name";
	private static final String INDEX3_NAME = "Index3Name";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void success_simple() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': true,"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd',"
									+ "'ignore_malformed': true" // Ignored during validation
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( INDEX2_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX2_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( INDEX3_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX3_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'index': true,"
									+ "'analyzer': 'default'"
							+ "},"
							+ "'NOTmyField': {" // Ignored during validation
									+ "'type': 'text',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		/*
		 TODO uncomment and adapt this once we restore support for faceting; see HSEARCH-3271

		elasticSearchClient.index( INDEX4_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX4_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd',"
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
					+ "}"
				+ "}"
				);
		 */

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.withIndex(
						"MappedType2", INDEX2_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.withIndex(
						"MappedType3", INDEX3_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"myField",
									f -> f.asString().analyzer( "default" )
							)
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
		// TODO add an index with a faceted field once we restore support for faceting; see HSEARCH-3271

		// If we get here, it means validation passed (no exception was thrown)
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tMissing type mapping" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'null'" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void rootMapping_attribute_dynamic_invalid() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void properties_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tMissing property mapping" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void property_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'NOTmyField': {"
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tMissing property mapping" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void property_attribute_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'object'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'type'. Expected 'DATE', actual is 'OBJECT'" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void property_attribute_invalid() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd',"
									+ "'index': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'index'. Expected 'true', actual is 'false'" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void property_analyzer_invalid() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'index': true,"
									+ "'analyzer': 'keyword'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'analyzer'. Expected 'default', actual is 'keyword'" )
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"myField",
									f -> f.asString().analyzer( "default" )
							)
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
	}

	@Test
	public void property_norms_invalid() throws Exception {
		Assume.assumeTrue( "Norms configuration is not supported yet; see HSEARCH-3048", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'text',"
									+ "'norms': false"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid value for attribute 'norms'. Expected 'true', actual is 'false'" )
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"myField",
									f -> f.asString().analyzer( "default" )
											// TODO disable norms once the APIs allow it; see HSEARCH-3048
							)
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
	}

	@Test
	public void property_format_invalidOutputFormat() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'epoch_millis||yyyy'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tThe output format (the first format in the 'format' attribute) is invalid. Expected 'strict_date', actual is 'epoch_millis'" )
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() )
									.createAccessor();
						},
						indexManager -> {
						}
				)
				.setup();
	}

	@Test
	public void property_format_missingInputFormat() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid formats for attribute 'format'" )
						.withMessage( "missing elements are '[yyyyyyyyy-MM-dd]'" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void property_format_unexpectedInputFormat() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd||yyyy'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField'" )
						.withMessage( "\n\tInvalid formats for attribute 'format'" )
						.withMessage( "unexpected elements are '[yyyy]'" )
				.build()
		);

		setupSimpleIndexWithLocalDateField( "MappedType1", INDEX1_NAME );
	}

	/**
	 * Tests that mappings that are more powerful than requested will pass validation.
	 */
	@Test
	public void property_attribute_leniency() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'long',"
									+ "'index': true,"
									+ "'store': true"
							+ "},"
							+ "'myTextField': {"
									+ "'type': 'text',"
									+ "'index': true,"
									+ "'norms': true"
							+ "}"
					+ "}"
				+ "}"
				);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLong() )
									.createAccessor();
							root.field( "myTextField", f -> f.asString().analyzer( "default" ) )
									.createAccessor();
						},
						indexManager -> {
						}
				)
				.setup();
	}

	@Test
	public void multipleErrors_singleIndexManagers() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'integer'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nindex '" + ElasticsearchIndexNameNormalizer.normalize( INDEX1_NAME ) + "':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
								+ "\nindex '" + ElasticsearchIndexNameNormalizer.normalize( INDEX1_NAME ) + "',"
								+ " property 'myField':"
								+ "\n\tInvalid value for attribute 'type'. Expected 'KEYWORD', actual is 'INTEGER'"
						)
				.build()
		);

		setupSimpleIndexWithKeywordField( "MappedType1", INDEX1_NAME );
	}

	@Test
	public void multipleErrors_multipleIndexManagers() throws Exception {
		Assume.assumeTrue(
				"Reporting validation failure for multiple indexes in the same bootstrap is not supported yet, see HSEARCH-3468",
				false
		);

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': false,"
					+ "'properties': {"
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
							+ "'myField': {"
									+ "'type': 'integer'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
				.withMainOrSuppressed(
						isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nindex '" + ElasticsearchIndexNameNormalizer.normalize( INDEX1_NAME ) + "':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
						)
						.build()
				)
				.withMainOrSuppressed(
						isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage(
								"\nindex '" + ElasticsearchIndexNameNormalizer.normalize( INDEX2_NAME ) + "':"
								+ "\n\tInvalid value for attribute 'dynamic'. Expected 'STRICT', actual is 'FALSE'"
								+ "\nindex '" + ElasticsearchIndexNameNormalizer.normalize( INDEX2_NAME ) + "',"
								+ " property 'myField':"
								+ "\n\tInvalid value for attribute 'type'. Expected 'KEYWORD', actual is 'INTEGER'"
						)
						.build()
				)
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.withIndex(
						"MappedType2", INDEX2_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
	}

	@Test
	public void property_fields_missing() throws Exception {
		Assume.assumeTrue( "Faceting/sub-fields are not supported yet; see HSEARCH-3465, HSEARCH-3271", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
									+ "'type': 'keyword',"
									+ "'store': true"
							+ "},"
							+ "'myField': {"
									+ "'type': 'date'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField', field 'myField" + FACET_FIELD_SUFFIX + "'" )
						.withMessage( "\n\tMissing field mapping" )
				.build()
		);

		withManagementStrategyConfiguration()
				// TODO add an index with a faceted field or sub-field when APIs allow it; see HSEARCH-3465, HSEARCH-3271
				.setup();
	}

	@Test
	public void property_field_missing() throws Exception {
		Assume.assumeTrue( "Faceting/sub-fields are not supported yet; see HSEARCH-3465, HSEARCH-3271", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
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
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField', field 'myField" + FACET_FIELD_SUFFIX + "'" )
						.withMessage( "\n\tMissing field mapping" )
				.build()
		);

		withManagementStrategyConfiguration()
				// TODO add an index with a faceted field or sub-field when APIs allow it; see HSEARCH-3465, HSEARCH-3271
				.setup();
	}

	@Test
	public void property_field_attribute_invalid() throws Exception {
		Assume.assumeTrue( "Faceting/sub-fields are not supported yet; see HSEARCH-3465, HSEARCH-3271", false );

		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'id': {"
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
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( VALIDATION_FAILED_MESSAGE_ID )
						.withMessage( "property 'myField', field 'myField" + FACET_FIELD_SUFFIX + "'" )
						.withMessage( "\n\tInvalid value for attribute 'index'. Expected 'true', actual is 'false'" )
				.build()
		);

		withManagementStrategyConfiguration()
				// TODO add an index with a faceted field or sub-field when APIs allow it; see HSEARCH-3465, HSEARCH-3271
				.setup();
	}

	private void setupSimpleIndexWithKeywordField(String typeName, String indexName) {
		withManagementStrategyConfiguration()
				.withIndex(
						typeName, indexName,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asString() )
									.createAccessor();
						},
						indexManager -> {
						}
				)
				.setup();
	}

	private void setupSimpleIndexWithLocalDateField(String typeName, String indexName) {
		withManagementStrategyConfiguration()
				.withIndex(
						typeName, indexName,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() )
									.createAccessor();
						},
						indexManager -> {
						}
				)
				.setup();
	}

	private SearchSetupHelper.SetupContext withManagementStrategyConfiguration() {
		return setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						ElasticsearchIndexLifecycleStrategyName.VALIDATE.getExternalName()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchAnalysisConfigurer() {
							@Override
							public void configure(ElasticsearchAnalysisDefinitionContainerContext context) {
								// No-op
							}
						}
				);
	}

}
