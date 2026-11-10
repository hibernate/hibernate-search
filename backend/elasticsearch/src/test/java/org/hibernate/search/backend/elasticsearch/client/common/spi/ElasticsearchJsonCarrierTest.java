package org.hibernate.search.backend.elasticsearch.client.common.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchJsonCarrierTest {
	@Test
	void nativePartsAreBorrowedAndListIsFrozenAcrossBuilderReuse() {
		ObjectNode source = JsonNodeFactory.instance.objectNode().put( "value", 1 );
		var builder = ElasticsearchJsonRequest.post().bodyJson( source );
		var request = builder.build();
		builder.bodyJson( JsonNodeFactory.instance.objectNode() );
		assertThat( request.bodyPartsJson() ).containsExactly( source );
		assertThat( request.bodyPartsJson().get( 0 ) ).isSameAs( source );
		assertThat( request.bodyParts( JacksonJsonHandlers.object() ).get( 0 ) ).isSameAs( source );
		assertThatThrownBy( () -> request.bodyPartsJson().clear() ).isInstanceOf( UnsupportedOperationException.class );
	}

	@Test
	@SuppressWarnings("deprecation")
	void explicitHandlersConvertOnDemandWithoutRetainingSecondTree() {
		var source = new JsonObject();
		source.add( "nested", JsonParser.parseString( "[9007199254740993,null,{\"null\":null}]" ) );
		var request = ElasticsearchJsonRequest.post()
				.bodyJson( JacksonJsonHandlers.object().fromJson( GsonJsonHandlers.object().toJson( source ) ) ).build();
		source.addProperty( "later", true );
		assertThat( request.bodyPartsJson().get( 0 ).has( "later" ) ).isFalse();
		var exported = request.bodyParts( GsonJsonHandlers.object() ).get( 0 );
		exported.addProperty( "mutated", true );
		assertThat( request.bodyParts( GsonJsonHandlers.object() ).get( 0 ).has( "mutated" ) ).isFalse();
		assertThatThrownBy( () -> request.bodyParts( GsonJsonHandlers.object() ).clear() )
				.isInstanceOf( UnsupportedOperationException.class );
		var response = ElasticsearchJsonResponse.withJsonBody( "host", 200, "ok", request.bodyPartsJson().get( 0 ) );
		assertThat( response.body( GsonJsonHandlers.object() ) )
				.isEqualTo( request.bodyParts( GsonJsonHandlers.object() ).get( 0 ) );
		assertThat( response.body( GsonJsonHandlers.object() ) ).isNotSameAs( response.body( GsonJsonHandlers.object() ) );
		assertThat( response.bodyJson().get( "nested" ).get( 0 ).longValue() ).isEqualTo( 9007199254740993L );
		assertThat( response.bodyJson().get( "nested" ).get( 1 ).isNull() ).isTrue();
	}

	@Test
	void optionalResponseAndCustomExportPreserveObjectContract() {
		var absent = ElasticsearchJsonResponse.withJsonBody( "host", 204, "empty", null );
		assertThat( absent.bodyJson() ).isNull();
		assertThat( absent.body( GsonJsonHandlers.object() ) ).isNull();
		var source = new JsonParsing().parse( "{\"nested\":{\"null\":null}}" ).asObject();
		var response = ElasticsearchJsonResponse.withJsonBody( "host", 200, "ok", source );
		assertThat( response.bodyJson() ).isSameAs( source );
		JsonHandler<String> handler = new JsonHandler<>() {
			public Class<String> javaType() {
				return String.class;
			}

			public String toJson(String value) {
				return value;
			}

			public String fromJson(String json) {
				return json;
			}
		};
		assertThat( response.body( handler ) ).isEqualTo( "{\"nested\":{\"null\":null}}" );
	}

	@Test
	void invalidNativeTreesFailBeforeRetention() {
		ObjectNode cycle = JsonNodeFactory.instance.objectNode();
		cycle.set( "cycle", cycle );
		assertThatThrownBy( () -> ElasticsearchJsonRequest.post().bodyJson( cycle ) ).isInstanceOf( RuntimeException.class );
		assertThatThrownBy( () -> ElasticsearchJsonResponse.withJsonBody( "host", 200, "ok", cycle ) )
				.isInstanceOf( RuntimeException.class );
		ObjectNode unsupported = JsonNodeFactory.instance.objectNode().putPOJO( "pojo", new Object() );
		assertThatThrownBy( () -> ElasticsearchJsonRequest.post().bodyJson( unsupported ) )
				.isInstanceOf( RuntimeException.class );
		assertThatThrownBy( () -> ElasticsearchJsonRequest.post().bodyJson( null ) ).isInstanceOf( RuntimeException.class );
	}

	@Test
	void emptyRequestValidatesHandlerAndTokenWithoutConverting() {
		var request = ElasticsearchJsonRequest.get().build();
		assertThat( request.bodyParts( JacksonJsonHandlers.object() ) ).isEmpty();
		assertThatThrownBy( () -> request.bodyParts( null ) )
				.isInstanceOf( SearchException.class ).hasCauseInstanceOf( NullPointerException.class );
		JsonHandler<ObjectNode> handler = new JsonHandler<>() {
			@Override
			public Class<ObjectNode> javaType() {
				return ObjectNode.class;
			}

			@Override
			public String toJson(ObjectNode value) {
				throw new AssertionFailure( "Empty export must not serialize" );
			}

			@Override
			public ObjectNode fromJson(String json) {
				throw new AssertionFailure( "Empty export must not parse" );
			}
		};
		assertThat( request.bodyParts( handler ) ).isEmpty();
		JsonHandler<ObjectNode> missingToken = new JsonHandler<>() {
			@Override
			public Class<ObjectNode> javaType() {
				return null;
			}

			@Override
			public String toJson(ObjectNode value) {
				throw new AssertionFailure( "Invalid token must fail first" );
			}

			@Override
			public ObjectNode fromJson(String json) {
				throw new AssertionFailure( "Invalid token must fail first" );
			}
		};
		assertThatThrownBy( () -> request.bodyParts( missingToken ) )
				.isInstanceOf( SearchException.class ).hasCauseInstanceOf( NullPointerException.class );
	}

}
