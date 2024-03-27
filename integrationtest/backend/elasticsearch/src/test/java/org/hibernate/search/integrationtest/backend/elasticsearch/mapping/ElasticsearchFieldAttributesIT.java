/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.integrationtest.backend.elasticsearch.mapping;

import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultAliases;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.ElasticsearchIndexMetadataTestUtils.defaultPrimaryName;

import java.util.function.Consumer;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.cfg.impl.ElasticsearchBackendImplSettings;
import org.hibernate.search.backend.elasticsearch.client.spi.ElasticsearchRequest;
import org.hibernate.search.engine.backend.document.model.dsl.IndexSchemaElement;
import org.hibernate.search.engine.backend.types.Norms;
import org.hibernate.search.engine.backend.types.TermVector;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchClientSpy;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchRequestAssertionMode;
import org.hibernate.search.integrationtest.backend.elasticsearch.testsupport.util.ElasticsearchTckBackendFeatures;
import org.hibernate.search.integrationtest.backend.tck.testsupport.util.extension.SearchSetupHelper;
import org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.dialect.ElasticsearchTestDialect;
import org.hibernate.search.util.impl.integrationtest.mapper.stub.StubMappedIndex;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.google.gson.JsonObject;

class ElasticsearchFieldAttributesIT {

	private final ElasticsearchTestDialect dialect = ElasticsearchTestDialect.get();

	@RegisterExtension
	public final SearchSetupHelper setupHelper = SearchSetupHelper.create();

	@RegisterExtension
	public ElasticsearchClientSpy clientSpy = ElasticsearchClientSpy.create();

	@Test
	void verifyNorms() {
		JsonObject properties = new JsonObject();
		properties.add( "keyword", fieldWithNorms( "keyword", false ) );
		properties.add( "text", fieldWithNorms( "text", true ) );
		properties.add( "norms", fieldWithNorms( "keyword", true ) );
		properties.add( "omitNorms", fieldWithNorms( "text", false ) );

		matchMapping( root -> {
			root.field( "keyword", f -> f.asString() ).toReference();
			root.field( "text", f -> f.asString().analyzer( "standard" ) ).toReference();
			root.field( "norms", f -> f.asString().norms( Norms.YES ) ).toReference();
			root.field( "omitNorms", f -> f.asString().analyzer( "standard" ).norms( Norms.NO ) ).toReference();
		}, properties );
	}

	@Test
	void verifyTermVector() {
		JsonObject properties = new JsonObject();
		properties.add( "no", fieldWithTermVector( "no" ) );
		properties.add( "yes", fieldWithTermVector( "yes" ) );
		properties.add( "implicit", fieldWithTermVector( "no" ) );
		properties.add( "moreOptions", fieldWithTermVector( "with_positions_offsets_payloads" ) );

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
		JsonObject nativeField = new JsonObject();
		nativeField.addProperty( "type", "half_float" );
		nativeField.addProperty( "ignore_malformed", true );

		JsonObject properties = new JsonObject();
		properties.add( "native", nativeField );

		matchMapping( root -> {
			root.field(
					"native",
					f -> f.extension( ElasticsearchExtension.get() )
							.asNative().mapping( "{\"type\": \"half_float\",\"ignore_malformed\": true}" )
			)
					.toReference();
		}, properties );
	}

	private void matchMapping(Consumer<IndexSchemaElement> mapping, JsonObject properties) {
		StubMappedIndex index = StubMappedIndex.ofNonRetrievable( mapping );
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
				ElasticsearchRequest.put()
						.pathComponent( defaultPrimaryName( index.name() ) )
						.body( createIndex( properties ) )
						.build(),
				ElasticsearchRequestAssertionMode.EXTENSIBLE
		);

		setupHelper.start()
				.withBackendProperty( ElasticsearchBackendImplSettings.CLIENT_FACTORY, clientSpy.factoryReference() )
				.withIndex( index )
				.setup();

		clientSpy.verifyExpectationsMet();
	}

	private JsonObject createIndex(JsonObject properties) {
		JsonObject payload = new JsonObject();

		JsonObject mappings = new JsonObject();
		payload.add( "mappings", mappings );

		mappings.add( "properties", properties );
		return payload;
	}

	private static JsonObject fieldWithNorms(String type, boolean value) {
		JsonObject field = new JsonObject();
		field.addProperty( "type", type );
		field.addProperty( "norms", value );
		return field;
	}

	private static JsonObject fieldWithTermVector(String termVector) {
		JsonObject field = new JsonObject();
		field.addProperty( "type", "text" );
		field.addProperty( "term_vector", termVector );
		return field;
	}
}
