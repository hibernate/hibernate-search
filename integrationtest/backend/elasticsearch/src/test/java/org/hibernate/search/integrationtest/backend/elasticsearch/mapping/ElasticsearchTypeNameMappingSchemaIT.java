/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
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
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;

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

	@ParameterizedTest(name = "{0}")
	@MethodSource("params")
	void schema(String strategyName, JsonObject expectedMappingContent) {
		if ( ElasticsearchTckBackendFeatures.supportsVersionCheck() ) {
			clientSpy.expectNext(
					ElasticsearchRequest.get().build(),
					ElasticsearchRequestAssertionMode.STRICT
			);
		}
		clientSpy.expectNext(
				ElasticsearchRequest.get()
						.multiValuedPathComponent( defaultAliases( index.name() ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);
		clientSpy.expectNext(
				indexCreationRequest( expectedMappingContent ),
				ElasticsearchRequestAssertionMode.STRICT
		);

		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendImplSettings.CLIENT_FACTORY, clientSpy.factoryReference()
				)
				.withBackendProperty(
						// Don't contribute any analysis definitions, it messes with our assertions
						ElasticsearchIndexSettings.ANALYSIS_CONFIGURER,
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

	private ElasticsearchRequest indexCreationRequest(JsonObject expectedMappingContent) {
		ElasticsearchRequest.Builder schemaRequestBuilder = ElasticsearchRequest.put()
				.pathComponent( defaultPrimaryName( index.name() ) )
				.body( indexCreationPayload( expectedMappingContent ) );

		return schemaRequestBuilder.build();
	}

	private JsonObject indexCreationPayload(JsonObject expectedMappingContent) {
		JsonObject payload = new JsonObject();

		payload.add( "aliases", defaultAliasDefinitions( index.name() ) );

		payload.add( "mappings", expectedMappingContent );

		payload.add( "settings", new JsonObject() );

		return payload;
	}
}
