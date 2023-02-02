/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForInitialization;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.simpleMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.PortedFromSearch5;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to the mapping when updating indexes.
 */
@PortedFromSearch5(original = "org.hibernate.search.elasticsearch.test.Elasticsearch5SchemaMigrationIT")
public class ElasticsearchIndexSchemaManagerUpdateMappingBaseIT {

	private static final String UPDATE_FAILED_MESSAGE_ID = "HSEARCH400035";
	private static final String MAPPING_CREATION_FAILED_MESSAGE_ID = "HSEARCH400020";
	private static final String ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID = "HSEARCH400007";

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void nothingToDo_1() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field( "myField", f -> f.asLocalDate() )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
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

		setupAndUpdateIndex( index );

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
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void nothingToDo_2() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field( "myField", f -> f.asBoolean() )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
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

		setupAndUpdateIndex( index );

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
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void nothingToDo_3() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
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

		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( index.name() ).type().putMapping(
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

		setupAndUpdateIndex( index );

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
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void mapping_missing() throws Exception {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field( "myField", f -> f.asBoolean() )
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate();

		setupAndUpdateIndex( index );

		assertJsonEquals(
				simpleMappingForExpectations(
						"'myField': {"
									+ "'type': 'boolean',"
									+ "'doc_values': false"
							+ "}"
				),
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void rootMapping_attribute_missing() throws Exception {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field( "myField", f -> f.asBoolean() )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
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

		setupAndUpdateIndex( index );

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
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void property_missing() throws Exception {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field( "myField", f -> f.asLocalDate() )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
				.deleteAndCreate()
				.type().putMapping(
						simpleMappingForInitialization(
								"'NOTmyField': {"
										+ "'type': 'date',"
										+ "'index': true"
								+ "}"
						)
				);

		setupAndUpdateIndex( index );

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
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void property_attribute_invalid() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field( "myField", f -> f.asLocalDate() )
						.toReference()
		);

		elasticSearchClient.index( index.name() )
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
				index,
				UPDATE_FAILED_MESSAGE_ID,
				MAPPING_CREATION_FAILED_MESSAGE_ID,
				ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
				"Elasticsearch request failed",
				"[index]"
		);
	}

	@Test
	public void property_attribute_invalid_conflictingAnalyzer() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field(
						"analyzer",
						f -> f.asString().analyzer( "customAnalyzer" )
				)
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
					"'analyzer': {"
							+ "'type': 'text',"
							+ "'analyzer': 'standard'" // Invalid
					+ "}"
				)
		);

		setupAndUpdateIndexIndexExpectingFailure(
				index,
				UPDATE_FAILED_MESSAGE_ID,
				MAPPING_CREATION_FAILED_MESSAGE_ID,
				ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
				"Elasticsearch request failed",
				"[analyzer]"
		);
	}

	@Test
	public void property_attribute_invalid_conflictingNormalizer() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root ->
				root.field(
						"normalizer",
						f -> f.asString().normalizer( "customNormalizer" )
				)
						.toReference()
		);

		elasticSearchClient.index( index.name() ).deleteAndCreate(
				"index.analysis", generateAnalysisSettings()
				);
		elasticSearchClient.index( index.name() ).type().putMapping(
				simpleMappingForInitialization(
						"'normalizer': {"
								+ "'type': 'keyword',"
								+ "'normalizer': 'customNormalizer2'" // Invalid
						+ "}"
				)
		);

		setupAndUpdateIndexIndexExpectingFailure(
				index,
				UPDATE_FAILED_MESSAGE_ID,
				MAPPING_CREATION_FAILED_MESSAGE_ID,
				ELASTICSEARCH_REQUEST_FAILED_MESSAGE_ID,
				"Elasticsearch request failed",
				"[normalizer]"
		);
	}

	private void setupAndUpdateIndexIndexExpectingFailure(
			StubMappedIndex index, String ... messageContent) {
		assertThatThrownBy( () -> setupAndUpdateIndex( index ) )
				.isInstanceOf( SearchException.class )
				.hasMessageContainingAll( messageContent );
	}

	private void setupAndUpdateIndex(StubMappedIndex index) {
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
