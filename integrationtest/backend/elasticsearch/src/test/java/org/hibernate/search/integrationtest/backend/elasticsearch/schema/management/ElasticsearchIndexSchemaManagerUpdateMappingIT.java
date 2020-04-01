/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.mapper.mapping.building.spi.IndexedEntityBindingContext;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingIndexManager;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.assertj.core.api.Assertions;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to the mapping when updating indexes.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaMigrationIT")
public class ElasticsearchIndexSchemaManagerUpdateMappingIT {

	private static final String UPDATE_FAILED_MESSAGE_ID = "HSEARCH400035";
	private static final String MAPPING_CREATION_FAILED_MESSAGE_ID = "HSEARCH400020";
	private static final String ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID = "HSEARCH400007";

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	private StubMappingIndexManager indexManager;

	@Test
	public void nothingToDo_1() {
		elasticSearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping(
						simpleMappingForInitialization(
								"'myField': {"
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
						)
				);

		setupAndUpdateIndex( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() )
					.toReference();
		} );

		assertJsonEquals(
				simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'date',"
								+ "'doc_values': false,"
								+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "',"
								+ "'ignore_malformed': true" // Assert it was not removed
						+ "},"
						+ "'NOTmyField': {" // Assert it was not removed
								+ "'type': 'date'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void nothingToDo_2() {
		elasticSearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping(
						simpleMappingForInitialization(
								"'myField': {"
										+ "'type': 'boolean',"
										+ "'doc_values': false,"
										+ "'index': true"
								+ "},"
								+ "'NOTmyField': {" // Ignored during migration
										+ "'type': 'boolean',"
										+ "'index': true"
								+ "}"
						)
				);

		setupAndUpdateIndex( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asBoolean() )
					.toReference();
		} );

		assertJsonEquals(
				simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'boolean',"
								+ "'doc_values': false"
						+ "},"
						+ "'NOTmyField': {" // Assert it was not removed
								+ "'type': 'boolean'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void nothingToDo_3() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
						simpleMappingForInitialization(
								"'defaultAnalyzer': {"
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
						)
				);

		setupAndUpdateIndex( ctx -> {
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
		} );

		assertJsonEquals(
				simpleMappingForExpectations(
						"'defaultAnalyzer': {"
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
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void mapping_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();

		setupAndUpdateIndex( ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "myField", f -> f.asBoolean() )
							.toReference();
				}
		);

		assertJsonEquals(
				simpleMappingForExpectations(
						"'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false"
							+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping(
						"{"
								// "dynamic" missing
								+ "'properties': {"
										+ defaultMetadataMappingAndCommaForInitialization()
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

		setupAndUpdateIndex( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asBoolean() )
					.toReference();
		} );

		assertJsonEquals(
				simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'boolean',"
								+ "'doc_values': false"
						+ "},"
						+ "'NOTmyField': {" // Assert it was not removed
								+ "'type': 'boolean'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void property_missing() throws Exception {
		elasticSearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping(
						simpleMappingForInitialization(
								"'NOTmyField': {"
										+ "'type': 'date',"
										+ "'index': true"
								+ "}"
						)
				);

		setupAndUpdateIndex( ctx -> {
			IndexSchemaElement root = ctx.getSchemaElement();
			root.field( "myField", f -> f.asLocalDate() )
					.toReference();
		} );

		assertJsonEquals(
				simpleMappingForExpectations(
						"'myField': {"
								+ "'type': 'date',"
								+ "'doc_values': false,"
								+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "'"
						+ "},"
						+ "'NOTmyField': {" // Assert it was not removed
								+ "'type': 'date'"
						+ "}"
				),
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void property_attribute_invalid() {
		elasticSearchClient.index( INDEX_NAME )
				.deleteAndCreate()
				.type().putMapping(
						simpleMappingForInitialization(
							"'myField': {"
									+ "'type': 'date',"
									+ "'index': false," // Invalid
									+ "'format': '" + elasticSearchClient.getDialect().getConcatenatedLocalDateDefaultMappingFormats() + "'"
							+ "}"
						)
				);

		setupAndUpdateIndexIndexExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field( "myField", f -> f.asLocalDate() )
							.toReference();
				},
				UPDATE_FAILED_MESSAGE_ID,
				MAPPING_CREATION_FAILED_MESSAGE_ID,
				ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
				"Elasticsearch request failed",
				"different [index]"
		);
	}

	@Test
	public void property_attribute_invalid_conflictingAnalyzer() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
					"'analyzer': {"
							+ "'type': 'text',"
							+ "'analyzer': 'standard'" // Invalid
					+ "}"
				)
		);

		setupAndUpdateIndexIndexExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field(
							"analyzer",
							f -> f.asString().analyzer( "customAnalyzer" )
					)
							.toReference();
				},
				UPDATE_FAILED_MESSAGE_ID,
				MAPPING_CREATION_FAILED_MESSAGE_ID,
				ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
				"Elasticsearch request failed",
				"different [analyzer]"
		);
	}

	@Test
	public void property_attribute_invalid_conflictingNormalizer() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				simpleMappingForInitialization(
						"'normalizer': {"
								+ "'type': 'keyword',"
								+ "'normalizer': 'customNormalizer2'" // Invalid
						+ "}"
				)
		);

		setupAndUpdateIndexIndexExpectingFailure(
				ctx -> {
					IndexSchemaElement root = ctx.getSchemaElement();
					root.field(
							"normalizer",
							f -> f.asString().normalizer( "customNormalizer" )
					)
							.toReference();
				},
				UPDATE_FAILED_MESSAGE_ID,
				MAPPING_CREATION_FAILED_MESSAGE_ID,
				ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
				"Elasticsearch request failed",
				"different [normalizer]"
		);
	}

	private void setupAndUpdateIndexIndexExpectingFailure(
			Consumer<? super IndexedEntityBindingContext> mappingContributor, String ... messageContent) {
		Assertions.assertThatThrownBy( () -> setupAndUpdateIndex( mappingContributor ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( messageContent );
	}

	private void setupAndUpdateIndex(Consumer<? super IndexedEntityBindingContext> mappingContributor) {
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

		Futures.unwrappedExceptionJoin( indexManager.getSchemaManager().createOrUpdate() );
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
