package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Test the property types defined on Elasticsearch server
 */
class ElasticsearchFieldTypesIT {

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public final ElasticsearchClientSpy clientSpy = ElasticsearchClientSpy.create();

	private final StubMappedIndex index = StubMappedIndex.ofNonRetrievable( IndexBinding::new );

	@Test
	void test() {
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
		clientSpy.expectNext( ElasticsearchJsonRequest.put()
				.pathComponent( defaultPrimaryName( index.name() ) )
				.bodyJson( indexCreationPayload() )
				.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start()
				.withBackendProperty(
						ElasticsearchBackendSettings.CLIENT_FACTORY, clientSpy.factoryReference()
				)
				.withIndex( index )
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private ObjectNode indexCreationPayload() {
		ObjectNode payload = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode mappings = new ObjectNode( JsonNodeFactory.instance );
		payload.set( "mappings", mappings );
		ObjectNode properties = new ObjectNode( JsonNodeFactory.instance );
		mappings.set( "properties", properties );

		properties.set( "keyword", type( "keyword" ) );
		properties.set( "text", type( "text" ) );
		properties.set( "integer", type( "integer" ) );
		properties.set( "long", type( "long" ) );
		properties.set( "boolean", type( "boolean" ) );
		properties.set( "byte", type( "byte" ) );
		properties.set( "short", type( "short" ) );
		properties.set( "float", type( "float" ) );
		properties.set( "double", type( "double" ) );
		properties.set( "unsupportedType", type( "half_float" ) );

		return payload;
	}

	private static ObjectNode type(String type) {
		ObjectNode field = new ObjectNode( JsonNodeFactory.instance );
		field.put( "type", type );
		return field;
	}

	private static class IndexBinding {
		IndexBinding(IndexSchemaElement root) {
			// string type + not analyzed => keyword
			root.field( "keyword", f -> f.asString() ).toReference();

			// string type + analyzed => text
			root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();

			root.field( "integer", f -> f.asInteger() ).toReference();
			root.field( "long", f -> f.asLong() ).toReference();
			root.field( "boolean", f -> f.asBoolean() ).toReference();
			root.field( "byte", f -> f.asByte() ).toReference();
			root.field( "short", f -> f.asShort() ).toReference();
			root.field( "float", f -> f.asFloat() ).toReference();
			root.field( "double", f -> f.asDouble() ).toReference();
			root.field(
					"unsupportedType",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( JacksonJsonHandlers.value() ).mapping( "{\"type\": \"half_float\"}" )
			)
					.toReference();
		}
	}
}
