package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliasDefinitions;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithDiscriminatorProperty;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.mappingWithoutAnyProperty;

import java.util.Arrays;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test the schema produced by type name mapping strategies.
 */
class ElasticsearchTypeNameMappingSchemaIT {

	public static List<? extends Arguments> params() {
		return Arrays.asList(
				Arguments.of( null, mappingWithDiscriminatorProperty( "_entity_type" ) ),
				Arguments.of( "index-name", mappingWithoutAnyProperty() ),
				Arguments.of( "discriminator", mappingWithDiscriminatorProperty( "_entity_type" ) )
		);
	}

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy clientSpy = ElasticsearchClientSpy.create();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	// For EJC and lambda arg
	@SuppressWarnings("unused")
	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void schema(String strategyName, ObjectNode expectedMappingContent) {
		if ( ElasticsearchTckBackendFeatures.supportsVersionCheck() ) {
			clientSpy.expectNext( ElasticsearchJsonRequest.get().build(),
					ElasticsearchRequestAssertionMode.STRICT
			);
		}
		clientSpy.expectNext( ElasticsearchJsonRequest.get()
				.multiValuedPathComponent( defaultAliases( index.name() ) )
				.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		clientSpy.expectNext(
				indexCreationRequest( expectedMappingContent ),
				ElasticsearchRequestAssertionMode.STRICT
		);
		// Don't contribute any analysis definitions, it messes with our assertions
		// Don't contribute any analysis definitions, it messes with our assertions
		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.CLIENT_FACTORY, clientSpy.factoryReference()
				)
				.withBackendProperty( ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty(
						ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY, strategyName
				)
				.withIndex( index )
				.setup();
		clientSpy.verifyExpectationsMet();
	}

	private ElasticsearchJsonRequest indexCreationRequest(ObjectNode expectedMappingContent) {
		ElasticsearchJsonRequest.Builder schemaRequestBuilder = ElasticsearchJsonRequest.put()
				.pathComponent( defaultPrimaryName( index.name() ) )
				.bodyJson( indexCreationPayload( expectedMappingContent ) );

		return schemaRequestBuilder.build();
	}

	private ObjectNode indexCreationPayload(ObjectNode expectedMappingContent) {
		ObjectNode payload = new ObjectNode( JsonNodeFactory.instance );

		payload.set( "aliases", defaultAliasDefinitions( index.name() ) );

		payload.set( "mappings", expectedMappingContent );

		payload.set( "settings", new ObjectNode( JsonNodeFactory.instance ) );

		return payload;
	}
}
