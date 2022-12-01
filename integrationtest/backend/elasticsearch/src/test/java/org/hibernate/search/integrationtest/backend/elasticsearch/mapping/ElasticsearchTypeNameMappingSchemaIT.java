/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.google.gson.JsonObject;

/**
 * Test the schema produced by type name mapping strategies.
 */
public class ElasticsearchTypeNameMappingSchemaIT {

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
		clientSpy.expectNext(
				ElasticsearchRequest.get().build(),
				ElasticsearchRequestAssertionMode.STRICT
		);
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
		Boolean includeTypeName = ElasticsearchTestDialect.get().getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			schemaRequestBuilder.param( "include_type_name", includeTypeName );
		}
		return schemaRequestBuilder.build();
	}

	private JsonObject indexCreationPayload(JsonObject expectedMappingContent) {
		JsonObject payload = new JsonObject();

		payload.add( "aliases", defaultAliasDefinitions( index.name() ) );

		JsonObject mappings = ElasticsearchTestDialect.get().getTypeNameForMappingAndBulkApi()
				// ES6 and below: the mapping has its own object node, child of "mappings"
				.map( name -> {
					JsonObject doc = new JsonObject();
					doc.add( name.original, expectedMappingContent );
					return doc;
				} )
				// ES7 and below: the mapping is the "mappings" node
				.orElse( expectedMappingContent );

		payload.add( "mappings", mappings );

		payload.add( "settings", new JsonObject() );

		return payload;
	}
}
