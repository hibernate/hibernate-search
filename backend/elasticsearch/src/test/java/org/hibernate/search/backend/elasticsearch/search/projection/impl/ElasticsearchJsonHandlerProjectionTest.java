package org.hibernate.search.backend.elasticsearch.search.projection.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.JsonHandler;
import org.hibernate.search.util.json.internal.spi.JsonValidation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

class ElasticsearchJsonHandlerProjectionTest {
	private final ElasticsearchSearchIndexScope<?> scope = mock( ElasticsearchSearchIndexScope.class );

	@BeforeEach
	void configureJson() {
		when( scope.userFacingCodec() ).thenReturn( JsonProviderHelper.create( false ).getCodecNoSerializeNulls() );
	}

	@Test
	void projectionsCaptureIndependentValidationPolicies() {
		var codec = mock( JsonCodecService.class );
		when( scope.userFacingCodec() ).thenReturn( codec );
		when( codec.validation() ).thenReturn( JsonValidation.of( true ) );
		var enabled = new ElasticsearchSourceProjection<>( scope, JacksonJsonHandlers.object() );
		when( codec.validation() ).thenReturn( JsonValidation.of( false ) );
		var disabled = new ElasticsearchSourceProjection<>( scope, JacksonJsonHandlers.object() );
		ObjectNode internal = JacksonJsonHandlers.object().fromJson( "{}" ).put( "invalid", Double.NaN );
		assertThat( disabled.transform( null, internal, null ) ).isSameAs( internal );
		assertThatThrownBy( () -> enabled.transform( null, internal, null ) ).isInstanceOf( SearchException.class );
		assertThat( disabled.transform( null, internal, null ) ).isSameAs( internal );
	}

	@Test
	void sourceAndRawHitAreExportedThroughBoundHandlerOnceAtProjection() {
		ObjectNode hit =
				JacksonJsonHandlers.object().fromJson( "{\"_source\":{\"exact\":9007199254740993,\"nested\":[null,true]}}" );
		var direct = new ElasticsearchSourceProjection<>( scope, JacksonJsonHandlers.object() );
		ObjectNode extracted = direct.extract( null, hit, (ObjectNode) hit.get( "_source" ), null );
		assertThat( direct.transform( null, extracted, null ) ).isSameAs( hit.get( "_source" ) );
		var gson = new ElasticsearchSourceProjection<>( scope, GsonJsonHandlers.object() );
		assertThat( gson.transform( null, extracted, null ).getAsJsonArray( "nested" ).get( 0 ).isJsonNull() ).isTrue();
		AtomicInteger calls = new AtomicInteger();
		JsonHandler<JsonNode> custom = new JsonHandler<>() {
			@Override
			public Class<JsonNode> javaType() {
				return JsonNode.class;
			}

			@Override
			public String toJson(JsonNode value) {
				return JacksonJsonHandlers.value().toJson( value );
			}

			@Override
			public JsonNode fromJson(String text) {
				calls.incrementAndGet();
				return JacksonJsonHandlers.object().fromJson( text ).put( "custom", true );
			}
		};
		var customHit = new ElasticsearchJsonHitProjection<>( scope, custom );
		ObjectNode raw = customHit.extract( null, hit, extracted, null );
		assertThat( calls.get() ).isZero();
		assertThat( customHit.transform( null, raw, null ).path( "custom" ).booleanValue() ).isTrue();
		assertThat( calls.get() ).isOne();
	}

	@ParameterizedTest
	@ValueSource(strings = { "[]", "[{}]", "1", "true", "\"text\"" })
	void sourceAndExplanationRejectPresentNonObjectsEvenForValueHandler(String value) {
		ObjectNode hit = JacksonJsonHandlers.object().fromJson( "{\"_source\":" + value + ",\"_explanation\":" + value + "}" );
		var source = new ElasticsearchSourceProjection<>( scope, JacksonJsonHandlers.value() );
		assertThatThrownBy( () -> source.extract( null, hit, null, null ) ).isInstanceOf( SearchException.class );
		var explanation = new ElasticsearchExplanationProjection<>( scope, JacksonJsonHandlers.value() );
		assertThatThrownBy( () -> explanation.extract( null, hit, null, null ) ).isInstanceOf( SearchException.class );
	}

	@Test
	void explicitNullSourceReturnsNullWhileNullExplanationIsRejected() {
		ObjectNode hit = JacksonJsonHandlers.object().fromJson( "{\"_source\":null,\"_explanation\":null}" );
		var source = new ElasticsearchSourceProjection<>( scope, JacksonJsonHandlers.object() );
		assertThat( source.transform( null, source.extract( null, hit, null, null ), null ) ).isNull();
		var explanation = new ElasticsearchExplanationProjection<>( scope, JacksonJsonHandlers.object() );
		assertThatThrownBy( () -> explanation.extract( null, hit, null, null ) ).isInstanceOf( SearchException.class );
	}

	@Test
	void optionalMissingSourceAndExplanationReturnJavaNull() {
		ObjectNode hit = JacksonJsonHandlers.object().fromJson( "{}" );
		var source = new ElasticsearchSourceProjection<>( scope, JacksonJsonHandlers.object() );
		assertThat( source.transform( null, source.extract( null, hit, null, null ), null ) ).isNull();
		var explanation = new ElasticsearchExplanationProjection<>( scope, JacksonJsonHandlers.object() );
		assertThat( explanation.transform( null, explanation.extract( null, hit, null, null ), null ) ).isNull();
	}
}
