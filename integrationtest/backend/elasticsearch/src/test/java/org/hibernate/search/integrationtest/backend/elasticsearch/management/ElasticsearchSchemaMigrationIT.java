/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.management;

import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.index.IndexLifecycleStrategyName;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.util.impl.integrationtest.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.common.FailureReportUtils;
import org.hibernate.search.util.impl.test.SubTest;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;

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
									+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
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
									"defaultAnalyzer",
									f -> f.asString().analyzer( "default" )
							)
									.toReference();
							root.field(
									"nonDefaultAnalyzer",
									f -> f.asString().analyzer( "customAnalyzer" )
							)
									.toReference();
							root.field(
									"normalizer",
									f -> f.asString().normalizer( "customNormalizer" )
							)
									.toReference();
						}
				)
				.setup();

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'doc_values': false,"
									+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
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
						INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean() )
									.toReference();
						}
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
						INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asBoolean() )
									.toReference();
						}
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
						INDEX1_NAME,
						ctx -> {
							IndexSchemaElement root = ctx.getSchemaElement();
							root.field( "myField", f -> f.asLocalDate() )
									.toReference();
						}
				)
				.setup();

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'properties': {"
							+ "'myField': {"
									+ "'type': 'date',"
									+ "'doc_values': false,"
									+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "'"
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
									+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "'"
							+ "}"
					+ "}"
				+ "}"
				);

		setupExpectingFailure(
				() -> withManagementStrategyConfiguration()
						.withIndex(
								INDEX1_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field( "myField", f -> f.asLocalDate() )
											.toReference();
								}
						)
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.multilineFailure(
								UPDATE_FAILED_MESSAGE_ID,
								MAPPING_CREATION_FAILED_MESSAGE_ID,
								ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
								"Elasticsearch request failed",
								"different [index]"
						)
						.build()
		);
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

		setupExpectingFailure(
				() -> withManagementStrategyConfiguration()
						.withIndex(
								INDEX1_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field(
											"analyzer",
											f -> f.asString().analyzer( "customAnalyzer" )
									)
											.toReference();
								}
						)
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.multilineFailure(
								UPDATE_FAILED_MESSAGE_ID,
								MAPPING_CREATION_FAILED_MESSAGE_ID,
								ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
								"Elasticsearch request failed",
								"different [analyzer]"
						)
						.build()
		);
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

		setupExpectingFailure(
				() -> withManagementStrategyConfiguration()
						.withIndex(
								INDEX1_NAME,
								ctx -> {
									IndexSchemaElement root = ctx.getSchemaElement();
									root.field(
											"normalizer",
											f -> f.asString().normalizer( "customNormalizer" )
									)
											.toReference();
								}
						)
						.setup(),
				FailureReportUtils.buildFailureReportPattern()
						.indexContext( INDEX1_NAME )
						.multilineFailure(
								UPDATE_FAILED_MESSAGE_ID,
								MAPPING_CREATION_FAILED_MESSAGE_ID,
								ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
								"Elasticsearch request failed",
								"different [normalizer]"
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

	private SearchSetupHelper.SetupContext withManagementStrategyConfiguration() {
		return setupHelper.start( BACKEND_NAME )
				.withIndexDefaultsProperty(
						BACKEND_NAME,
						ElasticsearchIndexSettings.LIFECYCLE_STRATEGY,
						IndexLifecycleStrategyName.UPDATE.getExternalRepresentation()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						new ElasticsearchAnalysisConfigurer() {
							@Override
							public void configure(ElasticsearchAnalysisConfigurationContext context) {
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
