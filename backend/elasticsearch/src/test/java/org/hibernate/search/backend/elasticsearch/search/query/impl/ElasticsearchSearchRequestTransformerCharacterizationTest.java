package org.hibernate.search.backend.elasticsearch.search.query.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.JsonHandler;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchSearchRequestTransformerCharacterizationTest {
	@Test
	void fallbackTransformerUsesBackendParserConstraintsAndKeepsOriginalOnFailure() {
		var original = request( JacksonJsonHandlers.object().fromJson( "{\"ok\":1}" ) );
		var defaults = org.hibernate.search.util.json.internal.spi.JsonParsingOptions.DEFAULT;
		var parsing = new org.hibernate.search.util.json.internal.spi.JsonParsing(
				new org.hibernate.search.util.json.internal.spi.JsonParsingOptions( defaults.maxDepth(),
						defaults.maxStringLength(),
						3, defaults.maxNumberLength(), defaults.maxInputChunkSize(), 0 ) );
		var transform = ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction(
				context -> context.transformBody( GsonJsonHandlers.object(), body -> {
					body.addProperty( "longname", true );
					return body;
				} ), org.hibernate.search.util.json.internal.spi.JsonValidation.of( false ), parsing );
		assertThatThrownBy( () -> transform.apply( original ) ).isInstanceOf( SearchException.class );
		assertThat( original.bodyPartsJson().get( 0 ).has( "longname" ) ).isFalse();
	}

	private static ObjectNode body() {
		return JacksonJsonHandlers.object()
				.fromJson( "{\"query\":{\"term\":{\"name\":\"original\"}},\"unknown\":[null,true]}" );
	}

	private static ElasticsearchJsonRequest request(ObjectNode body) {
		return ElasticsearchJsonRequest.builder( "POST" ).wholeEncodedPath( "/index/_search" )
				.param( "size", "10" ).bodyJson( body ).build();
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyBodyIsIndependentLazyAndStableAcrossRepeatedAccesses() {
		ObjectNode body = body();
		ElasticsearchJsonRequest original = request( body );
		Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> transform =
				ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {
					JsonObject copy = context.body();
					assertThat( context.body() ).isSameAs( copy );
					copy.getAsJsonObject( "query" ).getAsJsonObject( "term" ).addProperty( "name", "changed" );
					context.parametersMap().put( "size", "20" );
				} );
		ElasticsearchJsonRequest transformed = transform.apply( original );
		assertThat( body.path( "query" ).path( "term" ).path( "name" ).stringValue() ).isEqualTo( "original" );
		assertThat( original.parameters().get( "size" ) ).isEqualTo( "10" );
		assertThat( transformed.parameters().get( "size" ) ).isEqualTo( "20" );
		assertThat( transformed.bodyPartsJson().get( 0 ) ).isNotSameAs( body );
	}

	@Test
	void trustedTransformerInstallsItsIndependentCallbackTreeWithNoPostCopy() {
		ObjectNode body = body();
		AtomicReference<ObjectNode> captured = new AtomicReference<>();
		ElasticsearchJsonRequest transformed = ElasticsearchSearchRequestTransformerContextImpl
				.createTransformerFunction( context -> context.transformBody( JacksonJsonHandlers.object(), input -> {
					captured.set( input );
					input.put( "track_total_hits", true );
					return input;
				} ) ).apply( request( body ) );
		assertThat( captured.get() ).isNotSameAs( body );
		assertThat( transformed.bodyPartsJson().get( 0 ) ).isSameAs( captured.get() );
		assertThat( body.has( "track_total_hits" ) ).isFalse();
		assertThat( captured.get().get( "unknown" ) ).isEqualTo( body.get( "unknown" ) ).isNotSameAs( body.get( "unknown" ) );
	}

	@Test
	void chainedTypedEditsSeeStagedChangesAndDifferentReturnedTreeIsRetained() {
		ObjectNode replacement = JacksonJsonHandlers.object().fromJson( "{\"replacement\":true}" );
		AtomicReference<ObjectNode> captured = new AtomicReference<>();
		ElasticsearchJsonRequest transformed =
				ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {
					context.transformBody( JacksonJsonHandlers.object(), ignored -> replacement );
					context.transformBody( JacksonJsonHandlers.object(), current -> {
						assertThat( current.path( "replacement" ).booleanValue() ).isTrue();
						captured.set( current );
						return current.put( "next", true );
					} );
				} ).apply( request( body() ) );
		assertThat( transformed.bodyPartsJson().get( 0 ) ).isSameAs( captured.get() );
		assertThat( replacement.has( "next" ) ).isFalse();
	}

	@Test
	void callbackAndOuterFailureKeepOriginalBodyAndRequestMetadata() {
		ObjectNode body = body();
		ElasticsearchJsonRequest original = request( body );
		Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> callbackFailure =
				ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> context.transformBody(
						JacksonJsonHandlers.object(), input -> {
							input.put( "partial", true );
							throw new IllegalStateException( "failed" );
						} ) );
		assertThatThrownBy( () -> callbackFailure.apply( original ) ).isInstanceOf( SearchException.class )
				.hasCauseInstanceOf( IllegalStateException.class );
		assertThat( body.has( "partial" ) ).isFalse();
		Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> outerFailure =
				ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {
					context.path( "/other" );
					context.parametersMap().put( "size", "20" );
					context.transformBody( JacksonJsonHandlers.object(), input -> input.put( "staged", true ) );
					throw new IllegalStateException( "outer failed" );
				} );
		assertThatThrownBy( () -> outerFailure.apply( original ) ).isInstanceOf( IllegalStateException.class );
		assertThat( original.path() ).isEqualTo( "/index/_search" );
		assertThat( original.parameters().get( "size" ) ).isEqualTo( "10" );
		assertThat( body.has( "staged" ) ).isFalse();
	}

	@Test
	void returnedCycleIsRejectedRegardlessOfInternalValidationMode() {
		ElasticsearchJsonRequest original = request( body() );
		var transform = ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction(
				context -> context.transformBody( JacksonJsonHandlers.object(), input -> {
					input.set( "cycle", input );
					return input;
				} ) );
		assertThatThrownBy( () -> transform.apply( original ) ).isInstanceOf( SearchException.class )
				.hasCauseInstanceOf( IllegalArgumentException.class );
		assertThat( original.bodyPartsJson().get( 0 ).has( "cycle" ) ).isFalse();
	}

	@Test
	void customNodeHandlerConversionFailureKeepsPreviouslyStagedBody() {
		int[] calls = { 0, 0 };
		JsonHandler<JsonNode> custom = new JsonHandler<>() {
			@Override
			public Class<JsonNode> javaType() {
				return JsonNode.class;
			}

			@Override
			public String toJson(JsonNode value) {
				calls[0]++;
				return "[]";
			}

			@Override
			public JsonNode fromJson(String json) {
				calls[1]++;
				return JacksonJsonHandlers.value().fromJson( json );
			}
		};
		ElasticsearchJsonRequest transformed =
				ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {
					context.transformBody( JacksonJsonHandlers.object(), input -> input.put( "successful", true ) );
					assertThatThrownBy( () -> context.transformBody( custom, input -> {
						( (ObjectNode) input ).put( "failed", true );
						return input;
					} ) ).isInstanceOf( SearchException.class );
				} ).apply( request( body() ) );
		assertThat( transformed.bodyPartsJson().get( 0 ).path( "successful" ).booleanValue() ).isTrue();
		assertThat( transformed.bodyPartsJson().get( 0 ).has( "failed" ) ).isFalse();
		assertThat( calls ).containsExactly( 1, 1 );
	}

	@Test
	@SuppressWarnings("deprecation")
	void rejectsMixingLegacyAndTypedBodyEditingInBothOrders() {
		assertThatThrownBy( () -> ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {
			context.body();
			context.transformBody( GsonJsonHandlers.object(), input -> input );
		} ).apply( request( body() ) ) ).isInstanceOf( SearchException.class );
		assertThatThrownBy( () -> ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {
			context.transformBody( JacksonJsonHandlers.object(), input -> input );
			context.body();
		} ).apply( request( body() ) ) ).isInstanceOf( SearchException.class );
	}

	@Test
	void pathOnlySharesOriginalTreeAndBodyPartCountIsChecked() {
		ObjectNode body = body();
		ElasticsearchJsonRequest original = request( body );
		ElasticsearchJsonRequest pathOnly = ElasticsearchSearchRequestTransformerContextImpl
				.createTransformerFunction( context -> context.path( "/other" ) ).apply( original );
		assertThat( pathOnly.bodyPartsJson().get( 0 ) ).isSameAs( body );
		assertThat( ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( null ) ).isNull();
		Function<ElasticsearchJsonRequest, ElasticsearchJsonRequest> transform =
				ElasticsearchSearchRequestTransformerContextImpl.createTransformerFunction( context -> {} );
		assertThatThrownBy(
				() -> transform.apply( ElasticsearchJsonRequest.builder( "POST" ).wholeEncodedPath( "/_search" ).build() ) )
				.isInstanceOf( AssertionFailure.class );
		assertThatThrownBy( () -> transform.apply( ElasticsearchJsonRequest.builder( "POST" ).wholeEncodedPath( "/_search" )
				.bodyJson( body() ).bodyJson( body() ).build() ) ).isInstanceOf( AssertionFailure.class );
	}
}
