package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchFieldAttributesIT {

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy clientSpy = ElasticsearchClientSpy.create();

	@Test
	void verifyNorms() {
		ObjectNode properties = new ObjectNode( JsonNodeFactory.instance );
		properties.set( "keyword", fieldWithNorms( "keyword", false ) );
		properties.set( "text", fieldWithNorms( "text", true ) );
		properties.set( "norms", fieldWithNorms( "keyword", true ) );
		properties.set( "omitNorms", fieldWithNorms( "text", false ) );

		matchMapping( root -> {
			root.field( "keyword", f -> f.asString() ).toReference();
			root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();
			root.field( "norms", f -> f.asString().norms( Norms.YES ) ).toReference();
			root.field( "omitNorms", f -> f.asString().analyzer( "standard" ).norms( Norms.NO ) ).toReference();
		}, properties );
	}

	@Test
	void verifyTermVector() {
		ObjectNode properties = new ObjectNode( JsonNodeFactory.instance );
		properties.set( "no", fieldWithTermVector( "no" ) );
		properties.set( "yes", fieldWithTermVector( "yes" ) );
		properties.set( "implicit", fieldWithTermVector( "no" ) );
		properties.set( "moreOptions", fieldWithTermVector( "with_positions_offsets_payloads" ) );

		matchMapping( root -> {
			root.field( "no", f -> f.asString().analyzer( "standard" ).termVector( TermVector.NO ) ).toReference();
			root.field( "yes", f -> f.asString().analyzer( "standard" ).termVector( TermVector.YES ) ).toReference();
			root.field( "implicit", f -> f.asString().analyzer( "standard" ) ).toReference();
			root.field( "moreOptions",
					f -> f.asString().analyzer( "standard" ).termVector( TermVector.WITH_POSITIONS_OFFSETS_PAYLOADS ) )
					.toReference();
		}, properties );
	}

	@Test
	void verifyNative() {
		ObjectNode nativeField = new ObjectNode( JsonNodeFactory.instance );
		nativeField.put( "type", "half_float" );
		nativeField.put( "ignore_malformed", true );
		ObjectNode properties = new ObjectNode( JsonNodeFactory.instance );
		properties.set( "native", nativeField );

		matchMapping( root -> {
			root.field(
					"native",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative( JacksonJsonHandlers.value() )
							.mapping( "{\"type\": \"half_float\",\"ignore_malformed\": true}" )
			)
					.toReference();
		}, properties );
	}

	private void matchMapping(Consumer<IndexSchemaElement> mapping, ObjectNode properties) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( mapping );
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
				.bodyJson( createIndex( properties ) )
				.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start()
				.withBackendProperty( ElasticsearchBackendSettings.CLIENT_FACTORY, clientSpy.factoryReference() )
				.withIndex( index )
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private ObjectNode createIndex(ObjectNode properties) {
		ObjectNode payload = new ObjectNode( JsonNodeFactory.instance );
		ObjectNode mappings = new ObjectNode( JsonNodeFactory.instance );
		payload.set( "mappings", mappings );

		mappings.set( "properties", properties );
		return payload;
	}

	private static ObjectNode fieldWithNorms(String type, boolean value) {
		ObjectNode field = new ObjectNode( JsonNodeFactory.instance );
		field.put( "type", type );
		field.put( "norms", value );
		return field;
	}

	private static ObjectNode fieldWithTermVector(String termVector) {
		ObjectNode field = new ObjectNode( JsonNodeFactory.instance );
		field.put( "type", "text" );
		field.put( "term_vector", termVector );
		return field;
	}
}
