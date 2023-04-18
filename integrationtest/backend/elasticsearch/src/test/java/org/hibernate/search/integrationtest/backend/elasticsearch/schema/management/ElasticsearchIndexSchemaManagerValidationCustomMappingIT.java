/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;

import java.util.EnumSet;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.rule.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

/**
 * Tests related to index custom mapping when validating indexes,
 * for all index-validating schema management operations.
 */
@RunWith(Parameterized.class)
public class ElasticsearchIndexSchemaManagerValidationCustomMappingIT {

	@Parameterized.Parameters(name = "With operation {0}")
	public static EnumSet<ElasticsearchIndexSchemaManagerValidationOperation> operations() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all();
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public TestElasticsearchClient elasticsearchClient = new TestElasticsearchClient();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();
	private final ElasticsearchIndexSchemaManagerValidationOperation operation;

	public ElasticsearchIndexSchemaManagerValidationCustomMappingIT(
			ElasticsearchIndexSchemaManagerValidationOperation operation) {
		this.operation = operation;
	}

	@Test
	public void success() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
				"   'properties':{ " +
				"     '_entity_type':{ " +
				"        'type':'keyword', " +
				"        'index':false " +
				"     }, " +
				"     'userField':{ " +
				"       'type':'keyword', " +
				"       'index':true, " +
				"       'norms':true, " +
				"       'doc_values':true " +
				"     }, " +
				"     'userObject':{ " +
				"       'dynamic':'true', " +
				"       'type':'object' " +
				"     } " +
				"   }, " +
				"   '_source': { " +
				"     'enabled': false " +
				"   }, " +
				"   'dynamic':'strict' " +
				" } "
		);

		setupAndValidate( "no-overlapping.json" );
	}

	@Test
	public void wrongSource() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
				"   'properties':{ " +
				"     '_entity_type':{ " +
				"        'type':'keyword', " +
				"        'index':false " +
				"     }, " +
				"     'userField':{ " +
				"       'type':'keyword', " +
				"       'index':true, " +
				"       'norms':true, " +
				"       'doc_values':true " +
				"     }, " +
				"     'userObject':{ " +
				"       'dynamic':'true', " +
				"       'type':'object' " +
				"     } " +
				"   }, " +
				"   '_source': { " +
				"     'enabled': true " +
				"   }, " +
				"   'dynamic':'strict' " +
				" } "
		);

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json" ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.mappingAttributeContext( "_source" )
						// "_source" enabled is the default, so it is not presented by ES
						.failure( "Custom index mapping attribute missing" ) );
	}

	@Test
	public void missingSource() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
				"   'properties':{ " +
				"     '_entity_type':{ " +
				"        'type':'keyword', " +
				"        'index':false " +
				"     }, " +
				"     'userField':{ " +
				"       'type':'keyword', " +
				"       'index':true, " +
				"       'norms':true, " +
				"       'doc_values':true " +
				"     }, " +
				"     'userObject':{ " +
				"       'dynamic':'true', " +
				"       'type':'object' " +
				"     } " +
				"   }, " +
				"   'dynamic':'strict' " +
				" } "
		);

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json" ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.mappingAttributeContext( "_source" )
						.failure( "Custom index mapping attribute missing" ) );
	}

	@Test
	public void wrongField() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
				"   'properties':{ " +
				"     '_entity_type':{ " +
				"        'type':'keyword', " +
				"        'index':false " +
				"     }, " +
				"     'userField':{ " +
				"       'type':'integer', " +
				"       'index':false, " +
				"       'doc_values':false " +
				"     }, " +
				"     'userObject':{ " +
				"       'dynamic':'true', " +
				"       'type':'object' " +
				"     } " +
				"   }, " +
				"   '_source': { " +
				"     'enabled': false " +
				"   }, " +
				"   'dynamic':'strict' " +
				" } "
		);

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json" ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "userField" )
						.mappingAttributeContext( "type" )
						.failure( "Invalid value. Expected 'keyword', actual is 'integer'" )
						.mappingAttributeContext( "index" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" )
						.mappingAttributeContext( "norms" )
						.failure( "Invalid value. Expected 'true', actual is 'null'" )
						.mappingAttributeContext( "doc_values" )
						.failure( "Invalid value. Expected 'true', actual is 'false'" ) );
	}

	@Test
	public void missingField() {
		elasticsearchClient.index( index.name() ).deleteAndCreate();
		elasticsearchClient.index( index.name() ).type().putMapping(
				" { " +
				"   'properties':{ " +
				"     '_entity_type':{ " +
				"        'type':'keyword', " +
				"        'index':false " +
				"     }, " +
				"     'userObject':{ " +
				"       'dynamic':'true', " +
				"       'type':'object' " +
				"     } " +
				"   }, " +
				"   '_source': { " +
				"     'enabled': false " +
				"   }, " +
				"   'dynamic':'strict' " +
				" } "
		);

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json" ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "userField" )
						.failure( "Missing property mapping" ) );
	}

	private void setupAndValidate(String customMappingFile) {
		setupHelper.start()
				.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
				.withBackendProperty(
						// use an empty analysis configurer,
						// so that we have only the custom settings definitions
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withIndexProperty( index.name(), ElasticsearchIndexSettings.SCHEMA_MANAGEMENT_MAPPING_FILE,
						"custom-index-mapping/" + customMappingFile
				)
				.withIndex( index )
				.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}
}
