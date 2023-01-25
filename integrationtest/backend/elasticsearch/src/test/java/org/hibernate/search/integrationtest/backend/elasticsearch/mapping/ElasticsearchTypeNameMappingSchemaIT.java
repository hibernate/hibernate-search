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

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonObject;

/**
 * Test the schema produced by type name mapping strategies.
 */
@RunWith(Parameterized.class)
public class ElasticsearchTypeNameMappingSchemaIT {

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, mappingWithDiscriminatorProperty( "_entity_type" ) },
				{ "index-name", mappingWithoutAnyProperty() },
				{ "discriminator", mappingWithDiscriminatorProperty( "_entity_type" ) }
		};
	}

	@Rule
	public final SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	private final StubMappedIndex index = StubMappedIndex.withoutFields();

	private final String strategyName;
	private final JsonObject expectedMappingContent;

	public ElasticsearchTypeNameMappingSchemaIT(String strategyName, JsonObject expectedMappingContent) {
		this.strategyName = strategyName;
		this.expectedMappingContent = expectedMappingContent;
	}

	@Test
	public void schema() {
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
				indexCreationRequest(),
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

	private ElasticsearchRequest indexCreationRequest() {
		ElasticsearchRequest.Builder schemaRequestBuilder = ElasticsearchRequest.put()
				.pathComponent( defaultPrimaryName( index.name() ) )
				.body( indexCreationPayload() );
		Boolean includeTypeName = ElasticsearchTestDialect.get().getIncludeTypeNameParameterForMappingApi();
		if ( includeTypeName != null ) {
			schemaRequestBuilder.param( "include_type_name", includeTypeName );
		}
		return schemaRequestBuilder.build();
	}

	private JsonObject indexCreationPayload() {
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
