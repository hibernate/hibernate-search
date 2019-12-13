/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.integrationtest.backend.elasticsearch.mapping.ElasticsearchTypeNameMappingTestUtils.mappingWithDiscriminatorProperty;
import static org.hibernate.search.integrationtest.backend.elasticsearch.mapping.ElasticsearchTypeNameMappingTestUtils.mappingWithoutAnyProperty;

import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurationContext;
import org.hibernate.search.backend.elasticsearch.analysis.ElasticsearchAnalysisConfigurer;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.cfg.spi.ElasticsearchBackendSpiSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.rule.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.google.gson.JsonObject;

/**
 * Test the schema produced by type name mapping strategies.
 */
@RunWith(Parameterized.class)
public class ElasticsearchTypeNameMappingSchemaIT {

	private static final String BACKEND_NAME = "backendname";
	private static final String TYPE_NAME = "typename";
	private static final String INDEX_NAME = "indexname";

	@Parameterized.Parameters(name = "{0}")
	public static Object[][] configurations() {
		return new Object[][] {
				{ null, mappingWithDiscriminatorProperty( "__HSEARCH_type" ) },
				{ "index-name", mappingWithoutAnyProperty() },
				{ "discriminator", mappingWithDiscriminatorProperty( "__HSEARCH_type" ) }
		};
	}

	@Rule
	public SearchSetupHelper setupHelper = new SearchSetupHelper();

	@Rule
	public ElasticsearchClientSpy clientSpy = new ElasticsearchClientSpy();

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private final String strategyName;
	private final JsonObject expectedMappingContent;

	public ElasticsearchTypeNameMappingSchemaIT(String strategyName, JsonObject expectedMappingContent) {
		this.strategyName = strategyName;
		this.expectedMappingContent = expectedMappingContent;
	}

	@Test
	public void schema() {
		clientSpy.expectNext(
				ElasticsearchRequest.get().build(), ElasticsearchRequestAssertionMode.STRICT
		);

		clientSpy.expectNext(
				ElasticsearchRequest.head().pathComponent( URLEncodedString.fromString( INDEX_NAME ) ).build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		clientSpy.expectNext(
				ElasticsearchRequest.put()
						.pathComponent( URLEncodedString.fromString( INDEX_NAME ) )
						.body( indexCreationPayload() )
						.build(),
				ElasticsearchRequestAssertionMode.STRICT
		);

		setupHelper.start( BACKEND_NAME )
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSpiSettings.CLIENT_FACTORY, clientSpy.getFactory()
				)
				.withBackendProperty(
						BACKEND_NAME,
						// Don't contribute any analysis definitions, it messes with our assertions
						ElasticsearchBackendSettings.ANALYSIS_CONFIGURER,
						(ElasticsearchAnalysisConfigurer) (ElasticsearchAnalysisConfigurationContext context) -> {
							// No-op
						}
				)
				.withBackendProperty(
						BACKEND_NAME, ElasticsearchBackendSettings.MAPPING_TYPE_NAME_STRATEGY, strategyName
				)
				.withIndex(
						INDEX_NAME,
						options -> options.mappedType( TYPE_NAME ),
						ignored -> { },
						ignored -> { }
				)
				.setup();
		clientSpy.verifyExpectationsMet();
	}

	private JsonObject indexCreationPayload() {
		JsonObject payload = new JsonObject();

		JsonObject mappings = ElasticsearchTestDialect.get().getTypeNameForMappingApi()
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
