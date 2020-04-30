/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingForInitialization;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.document.model.dsl.ObjectFieldStorage;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests related to the mapping when updating indexes and field templates are defined.
 */
public class ElasticsearchIndexSchemaManagerUpdateMappingFieldTemplateIT {

	private static final String INDEX_NAME = "IndexName";

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticSearchClient = new TestElasticsearchClient();

	@Test
	public void nothingToDo() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*_t1',"
									+ "'mapping': { 'type': 'integer', 'doc_values': false }"
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*_t2',"
									+ "'mapping': { 'type': 'text', 'analyzer': 'default' }"
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*_t3',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} },"
							+ "{ 'myTemplate4': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'nested' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() )
					.matchingPathGlob( "*_t1" );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) )
					.matchingPathGlob( "*_t2" );
			root.fieldTemplate( "myTemplate3", f -> f.asString() )
					.matchingPathGlob( "*_t3" );
			root.objectFieldTemplate( "myTemplate4", ObjectFieldStorage.NESTED );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*_t1',"
									+ "'mapping':" + integerMappingForExpectations()
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*_t2',"
									+ "'mapping':" + textMappingForExpectations()
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*_t3',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} },"
							+ "{ 'myTemplate4': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'nested', 'dynamic': 'true' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'integer', 'doc_values': false }"
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) );
			root.fieldTemplate( "myTemplate3", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping':" + integerMappingForExpectations()
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*',"
									+ "'mapping':" + textMappingForExpectations()
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void extra() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'integer', 'doc_values': false }"
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'text', 'analyzer': 'default' }"
							+ "} },"
							+ "{ 'extraTemplate': {"
									+ "'path_match': '*_extra',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) );
			root.fieldTemplate( "myTemplate3", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping':" + integerMappingForExpectations()
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*',"
									+ "'mapping':" + textMappingForExpectations()
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void wrongOrder() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate2': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'text', 'analyzer': 'default' }"
							+ "} },"
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'integer', 'doc_values': false }"
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
			root.fieldTemplate( "myTemplate2", f -> f.asString().analyzer( "default" ) );
			root.fieldTemplate( "myTemplate3", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping':" + integerMappingForExpectations()
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*',"
									+ "'mapping':" + textMappingForExpectations()
							+ "} },"
							+ "{ 'myTemplate3': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void duplicate() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'integer', 'doc_values': false }"
							+ "} },"
							+ "{ 'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate1", f -> f.asInteger() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'path_match': '*',"
									+ "'mapping':" + integerMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_pathMatch_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'match_mapping_type': 'object',"
									+ "'mapping': { 'type': 'nested' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_suffix" );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': '*_suffix',"
									+ "'mapping': { 'type': 'nested', 'dynamic': 'true' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_pathMatch_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': '*_suffix2',"
									+ "'mapping': { 'type': 'nested' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectFieldStorage.NESTED )
					.matchingPathGlob( "*_suffix" );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': '*_suffix',"
									+ "'mapping': { 'type': 'nested', 'dynamic': 'true' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_pathMatch_extra() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*_suffix',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_matchMappingType_missing() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'nested' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectFieldStorage.NESTED );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'match_mapping_type': 'object',"
									+ "'mapping': { 'type': 'nested', 'dynamic': 'true' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_matchMappingType_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'match_mapping_type': 'long',"
									+ "'mapping': { 'type': 'nested' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.objectFieldTemplate( "myTemplate", ObjectFieldStorage.NESTED );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'match_mapping_type': 'object',"
									+ "'mapping': { 'type': 'nested', 'dynamic': 'true' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_matchMappingType_extra() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'match_mapping_type': 'long',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void attribute_extra() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'match': '*_suffix',"
									+ "'mapping': { 'type': 'keyword' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	@Test
	public void mapping_invalid() {
		elasticSearchClient.index( INDEX_NAME ).deleteAndCreate();
		elasticSearchClient.index( INDEX_NAME ).type().putMapping(
				"{"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'mapping': { 'type': 'integer' }"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForInitialization()
					+ "}"
				+ "}"
		);

		setupAndUpdate( root -> {
			root.fieldTemplate( "myTemplate", f -> f.asString() );
		} );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate': {"
									+ "'path_match': '*',"
									+ "'mapping':" + keywordMappingForExpectations()
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( INDEX_NAME ).type().getMapping()
		);
	}

	private void setupAndUpdate(Consumer<? super IndexSchemaElement> binder) {
		StubMappedIndex index = StubMappedIndex.withoutRetrievableBinding( INDEX_NAME, binder );

		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( index.getSchemaManager().createOrUpdate() );
	}

	private String integerMappingForExpectations() {
		return "{"
				+ "'type': 'integer',"
				+ "'doc_values': false,"
				+ "'index': true,"
				+ "'store': false"
				+ "}";
	}

	private String textMappingForExpectations() {
		return "{"
				+ "'type': 'text',"
				+ "'analyzer': 'default',"
				+ "'index': true,"
				+ "'norms': true,"
				+ "'store': false,"
				+ "'term_vector': 'no'"
				+ "}";
	}

	private String keywordMappingForExpectations() {
		return "{"
				+ "'type': 'keyword',"
				+ "'doc_values': false,"
				+ "'index': true,"
				+ "'norms': false,"
				+ "'store': false"
				+ "}";
	}
}
