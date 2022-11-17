/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingAndCommaForExpectations;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.defaultMetadataMappingForExpectations;
import static org.hibernate.search.util.impl.test.JsonHelper.assertJsonEquals;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Tests related to the mapping's field templates when creating indexes,
 * for all index-creating schema management operations.
 */
@RunWith(Parameterized.class)
public class ElasticsearchIndexSchemaManagerCreationMappingFieldTemplatesIT {

	@Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerOperation> operations() {
		return ElasticsearchIndexSchemaManagerOperation.creating();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final ElasticsearchIndexSchemaManagerOperation operation;

	public ElasticsearchIndexSchemaManagerCreationMappingFieldTemplatesIT(ElasticsearchIndexSchemaManagerOperation operation) {
		this.operation = operation;
	}

	@Test
	public void rootFieldTemplates() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			root.objectFieldTemplate( "myTemplate1", ObjectStructure.NESTED )
					.matchingPathGlob( "*_obj" );
			root.fieldTemplate( "myTemplate2", f -> f.asString() )
					.matchingPathGlob( "*_kw" );
		} );

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( index );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'true',"
					+ "'dynamic_templates': ["
							+ "{'myTemplate1': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': '*_obj',"
									+ "'mapping': {"
											+ "'type': 'nested',"
											+ "'dynamic': 'true'"
									+ "}"
							+ "} },"
							+ "{'myTemplate2': {"
									+ "'path_match': '*_kw',"
									+ "'mapping': {"
											+ "'type': 'keyword',"
											+ "'doc_values': false,"
											+ "'index': true,"
											+ "'norms': false"
									+ "}"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingForExpectations()
					+ "}"
				+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	@Test
	public void nonRootFieldTemplates() {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
			IndexSchemaObjectField objectField = root.objectField( "staticObject" );
			objectField.toReference();
			objectField.objectFieldTemplate( "myTemplate1", ObjectStructure.NESTED )
					.matchingPathGlob( "*_obj" );
			objectField.fieldTemplate( "myTemplate2", f -> f.asString() )
					.matchingPathGlob( "*_kw" );
		} );

		elasticSearchClient.index( index.name() )
				.ensureDoesNotExist().registerForCleanup();

		setupAndCreateIndex( index );

		assertJsonEquals(
				"{"
					+ "'dynamic': 'strict',"
					+ "'dynamic_templates': ["
							+ "{'staticObject.myTemplate1': {"
									+ "'match_mapping_type': 'object',"
									+ "'path_match': 'staticObject.*_obj',"
									+ "'mapping': {"
											+ "'type': 'nested',"
											+ "'dynamic': 'true'"
									+ "}"
							+ "} },"
							+ "{'staticObject.myTemplate2': {"
									+ "'path_match': 'staticObject.*_kw',"
									+ "'mapping': {"
											+ "'type': 'keyword',"
											+ "'doc_values': false,"
											+ "'index': true,"
											+ "'norms': false"
									+ "}"
							+ "} }"
					+ "],"
					+ "'properties': {"
							+ defaultMetadataMappingAndCommaForExpectations()
							+ "'staticObject': {"
									+ "'type': 'object',"
									+ "'dynamic': 'true'"
							+ "}"
					+ "}"
				+ "}",
				elasticSearchClient.index( index.name() ).type().getMapping()
		);
	}

	private void setupAndCreateIndex(StubMappedIndex index) {
		setupHelper.start()
				.withIndex( index )
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// Don't contribute any analysis definitions, migration of those is tested in another test class
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.setup();

		operation.apply( index.schemaManager() ).join();
	}

}
