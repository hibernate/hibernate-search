/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.integrationtest.backend.elasticsearch.schema.management.ElasticsearchIndexSchemaManagerTestUtils.hasValidationFailureReport;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Tests related to index custom mapping when validating indexes,
 * for all index-validating schema management operations.
 */
class ElasticsearchIndexSchemaManagerValidationCustomMappingIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerValidationOperation.all().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticsearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void success(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		setupAndValidate( "no-overlapping.json", operation );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void wrongSource(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json", operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.mappingAttributeContext( "_source" )
						// "_source" enabled is the default, so it is not presented by ES
						.failure( "Custom index mapping attribute missing" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void missingSource(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json", operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.mappingAttributeContext( "_source" )
						.failure( "Custom index mapping attribute missing" ) );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void wrongField(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json", operation ) )
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

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void missingField(ElasticsearchIndexSchemaManagerValidationOperation operation) {
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

		assertThatThrownBy( () -> setupAndValidate( "no-overlapping.json", operation ) )
				.isInstanceOf( SearchException.class )
				.satisfies( hasValidationFailureReport()
						.indexFieldContext( "userField" )
						.failure( "Missing property mapping" ) );
	}

	private void setupAndValidate(String customMappingFile, ElasticsearchIndexSchemaManagerValidationOperation operation) {
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
