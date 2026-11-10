package org.hibernate.search.integrationtest.backend.elasticsearch.schema.management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.index.DynamicMapping;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaObjectField;
import org.hibernate.search.engine.backend.types.ObjectStructure;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappingSchemaManagementStrategy;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.node.ObjectNode;

/**
 * Tests for the dynamic mapping attribute,
 * for all creating schema management operations.
 */
@TestForIssue(jiraKey = "HSEARCH-3122")
class ElasticsearchIndexSchemaManagerDynamicMappingIT {

	public static List<? extends Arguments> params() {
		return ElasticsearchIndexSchemaManagerOperation.creating().stream()
				.map( Arguments::of )
				.collect( Collectors.toList() );
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public TestElasticsearchClient elasticSearchClient = TestElasticsearchClient.create();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( root -> {
		root.field( "field", f -> f.asString() ).toReference();

		IndexSchemaObjectField nested = root.objectField( "nested", ObjectStructure.NESTED );
		nested.toReference();
		nested.field( "field", f -> f.asInteger() ).toReference();
	} );

	private final JsonCodecService json =
			JsonProviderHelper.create( false ).getCodec();

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void dynamicMapping_default(ElasticsearchIndexSchemaManagerOperation operation) {
		setupAndInspectIndex( null, operation );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.STRICT );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void dynamicMapping_strict(ElasticsearchIndexSchemaManagerOperation operation) {
		setupAndInspectIndex( DynamicMapping.STRICT.externalRepresentation(), operation );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.STRICT );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void dynamicMapping_false(ElasticsearchIndexSchemaManagerOperation operation) {
		setupAndInspectIndex( DynamicMapping.FALSE.externalRepresentation(), operation );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.FALSE );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void dynamicMapping_true(ElasticsearchIndexSchemaManagerOperation operation) {
		setupAndInspectIndex( DynamicMapping.TRUE.externalRepresentation(), operation );
		String mapping = elasticSearchClient.index( index.name() ).type().getMapping();

		verifyDynamicMapping( mapping, DynamicMapping.TRUE );
	}

	@ParameterizedTest(name = "With operation {0}")
	@MethodSource("params")
	void dynamicMapping_invalid(ElasticsearchIndexSchemaManagerOperation operation) {
		assertThatThrownBy(
				() -> setupAndInspectIndex( "invalid", operation )
		)
				.isInstanceOf( SearchException.class )
				.hasMessageContaining(
						"Invalid value for configuration property 'hibernate.search.backend.indexes.indexName.dynamic_mapping': 'invalid'"
				)
				.hasMessageContaining( "Valid values are: [true, false, strict]" );
	}

	// For EJC and lambda arg
	@SuppressWarnings("unused")
	private void setupAndInspectIndex(String dynamicMapping, ElasticsearchIndexSchemaManagerOperation operation) {
		// Don't contribute any analysis definitions, validation of those is tested in another test class
		SearchSetupHelper.SetupContext // Don't contribute any analysis definitions, validation of those is tested in another test class
		setupContext = // Don't contribute any analysis definitions, validation of those is tested in another test class
				setupHelper.start()
						.withBackendProperty( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
								(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
									// No-op
								}
						)
						.withSchemaManagement( StubMappingSchemaManagementStrategy.DROP_ON_SHUTDOWN_ONLY )
						.withIndex( index );

		if ( dynamicMapping != null ) {
			setupContext.withIndexProperty( index.name(), "dynamic_mapping", dynamicMapping );
		}

		setupContext.setup();

		Futures.unwrappedExceptionJoin( operation.apply( index.schemaManager() ) );
	}

	private void verifyDynamicMapping(String mapping, DynamicMapping dynamicMapping) {
		var tree = json.decode( mapping, ObjectNode.class );
		assertThat( JsonScalarConversions.asString( tree.get( "dynamic" ) ) )
				.isEqualTo( dynamicMapping.externalRepresentation() );
		assertThat( JsonScalarConversions
				.asString( ( (ObjectNode) ( (ObjectNode) tree.get( "properties" ) ).get( "nested" ) ).get( "dynamic" ) ) )
				.isEqualTo( dynamicMapping.externalRepresentation() );
	}
}
