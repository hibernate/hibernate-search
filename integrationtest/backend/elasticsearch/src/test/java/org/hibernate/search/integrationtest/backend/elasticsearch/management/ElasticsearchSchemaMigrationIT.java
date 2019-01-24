/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.util.impl.test.ExceptionMatcherBuilder.isException;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.model.dsl.ElasticsearchAnalysisDefinitionContainerContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexManagementStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.SearchException;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Tests for the schema migration feature when using automatic index management.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaMigrationIT")
public class ElasticsearchSchemaMigrationIT {

	private static final String UPDATE_FAILED_MESSAGE_ID = "HSEARCH400035";
	private static final String MAPPING_CREATION_FAILED_MESSAGE_ID = "HSEARCH400020";
	private static final String ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID = "HSEARCH400007";

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
	public void nothingToDo() throws Exception {
		elasticSearchClient.index( INDEX1_NAME )
				.deleteAndCreate()
				.type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': true,"
									+ "'doc_values': false,"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd',"
									+ "'ignore_malformed': true" // Ignored during migration
							+ "},"
							+ "'NOTmyField': {" // Ignored during migration
									+ "'type': 'date',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( INDEX2_NAME )
				.deleteAndCreate()
				.type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false,"
									+ "'index': true"
							+ "},"
							+ "'NOTmyField': {" // Ignored during migration
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);
		elasticSearchClient.index( INDEX3_NAME ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( INDEX3_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'defaultAnalyzer': {"
									+ "'type': 'text'"
							+ "},"
							+ "'nonDefaultAnalyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customAnalyzer'"
							+ "},"
							+ "'normalizer': {"
									+ "'type': 'keyword',"
									+ "'doc_values': false,"
									+ "'normalizer': 'customNormalizer'"
							+ "}"
					+ "}"
				+ "}"
				);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType1", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate().toIndexFieldType() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.withIndex(
						"MappedType2", INDEX2_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean().toIndexFieldType() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.withIndex(
						"MappedType3", INDEX3_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"defaultAnalyzer",
									f -> f.asString().analyzer( "default" ).toIndexFieldType()
							)
									.createAccessor();
							root.field(
									"nonDefaultAnalyzer",
									f -> f.asString().analyzer( "customAnalyzer" )
											.toIndexFieldType()
							)
									.createAccessor();
							root.field(
									"normalizer",
									f -> f.asString().normalizer( "customNormalizer" )
											.toIndexFieldType()
							)
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'doc_values': false,"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd',"
									+ "'ignore_malformed': true" // Assert it was not removed
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'date'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX1_NAME ).type().getMapping()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false"
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'boolean'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX2_NAME ).type().getMapping()
				);
		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'defaultAnalyzer': {"
									+ "'type': 'text'"
							+ "},"
							+ "'nonDefaultAnalyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'customAnalyzer'"
							+ "},"
							+ "'normalizer': {"
									+ "'type': 'keyword',"
									+ "'doc_values': false,"
									+ "'normalizer': 'customNormalizer'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX3_NAME ).type().getMapping()
				);
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate();

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean().toIndexFieldType() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX1_NAME ).type().getMapping()
				);
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME )
				.deleteAndCreate()
				.type().putMapping(
				"{"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false,"
									+ "'index': true"
							+ "},"
							+ "'NOTmyField': {"
									+ "'type': 'boolean',"
									+ "'index': true"
							+ "}"
					+ "}"
				+ "}"
				);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean().toIndexFieldType() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false"
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'boolean'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX1_NAME ).type().getMapping()
				);
	}

	@Test
	public void property_missing() throws Exception {
		elasticSearchClient.index( INDEX1_NAME )
				.deleteAndCreate()
				.type().putMapping(
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

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate().toIndexFieldType() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'doc_values': false,"
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
							+ "},"
							+ "'NOTmyField': {" // Assert it was not removed
									+ "'type': 'date'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX1_NAME ).type().getMapping()
				);
	}

	@Test
	public void property_attribute_invalid() {
		elasticSearchClient.index( INDEX1_NAME )
				.deleteAndCreate()
				.type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'index': false," // Invalid
									+ "'format': 'strict_date||yyyyyyyyy-MM-dd'"
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( UPDATE_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( MAPPING_CREATION_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID )
						.withMessage( "index" )
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate().toIndexFieldType() )
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
	}

	@Test
	public void property_attribute_invalid_conflictingAnalyzer() {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'analyzer': {"
									+ "'type': 'text',"
									+ "'analyzer': 'standard'" // Invalid
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( UPDATE_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( MAPPING_CREATION_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID )
						.withMessage( "analyzer" )
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"analyzer",
									f -> f.asString().analyzer( "customAnalyzer" )
											.toIndexFieldType()
							)
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
	}

	@Test
	public void property_attribute_invalid_conflictingNormalizer() {
		elasticSearchClient.index( INDEX1_NAME ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( INDEX1_NAME ).type().putMapping(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'normalizer': {"
									+ "'type': 'keyword',"
									+ "'normalizer': 'customNormalizer2'" // Invalid
							+ "}"
					+ "}"
				+ "}"
				);

		thrown.expect(
				isException( SearchException.class )
						.withMessage( UPDATE_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( MAPPING_CREATION_FAILED_MESSAGE_ID )
				.causedBy( SearchException.class )
						.withMessage( ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID )
						.withMessage( "normalizer" )
				.build()
		);

		withManagementStrategyConfiguration()
				.withIndex(
						"MappedType", INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field(
									"normalizer",
									f -> f.asString().normalizer( "customNormalizer" )
											.toIndexFieldType()
							)
									.createAccessor();
						},
						indexManager -> { }
				)
				.setup();
	}

	private SearchSetupHelper.SetupContext withManagementStrategyConfiguration() {
		return setupHelper.withDefaultConfiguration( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.MANAGEMENT_STRATEGY,
						ElasticsearchIndexManagementStrategyName.UPDATE.getExternalName()
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

	private String generateAnalysisSettings() {
		return "{"
					+ "'analyzer': {"
						+ "'customAnalyzer': {"
								+ "'type': 'keyword'"
						+ "}"
					+ "},"
					+ "'normalizer': {"
						+ "'customNormalizer': {"
								+ "'filter': ['asciifolding']"
						+ "},"
						+ "'customNormalizer2': {"
								+ "'filter': ['asciifolding']"
						+ "}"
					+ "}"
				+ "}";
	}

}
