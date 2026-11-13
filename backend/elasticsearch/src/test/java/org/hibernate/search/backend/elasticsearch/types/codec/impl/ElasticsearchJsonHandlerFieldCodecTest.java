package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.common.SearchException;
import org.hibernate.search.util.json.JsonHandler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.MissingNode;
import tools.jackson.databind.node.NullNode;

class ElasticsearchJsonHandlerFieldCodecTest {
	private final JsonCodecService json = JsonProviderHelper.create( false ).getCodec();

	@ParameterizedTest
	@ValueSource(strings = {
			"{}",
			"[9007199254740993,null,{\"nested\":null}]",
			"true",
			"\"text\"",
			"null",
			"123456789012345678901234567890",
			"0.123456789012345678901234567890",
			"-0.0" })
	void indexingAndProjectionUseBoundTypeWithFieldLevelNullConvention(String text) {
		var jackson = new ElasticsearchJsonHandlerFieldCodec<>( json, JacksonJsonHandlers.value() );
		JsonNode value = JacksonJsonHandlers.value().fromJson( text );
		assertThat( jackson.javaType() ).isEqualTo( JsonNode.class );
		assertThat( jackson.encode( value ) ).isSameAs( value );
		assertThat( jackson.decode( value ) ).isSameAs( value.isNull() ? null : value );
		assertThat( jackson.canDecodeArrays() ).isFalse();
		var gson = new ElasticsearchJsonHandlerFieldCodec<>( json, GsonJsonHandlers.value() );
		var gsonValue = GsonJsonHandlers.value().fromJson( text );
		JsonNode indexed = gson.encode( gsonValue );
		assertThat( indexed ).isEqualTo( value );
		assertThat( gson.decode( indexed ) ).isEqualTo( value.isNull() ? null : gsonValue );
	}

	@Test
	void absentNullAndMetricResultsKeepRepresentationAndCustomBehavior() {
		AtomicInteger imports = new AtomicInteger();
		AtomicInteger exports = new AtomicInteger();
		JsonHandler<JsonNode> custom = new JsonHandler<>() {
			@Override
			public Class<JsonNode> javaType() {
				return JsonNode.class;
			}

			@Override
			public String toJson(JsonNode value) {
				imports.incrementAndGet();
				return JacksonJsonHandlers.value().toJson( value );
			}

			@Override
			public JsonNode fromJson(String text) {
				exports.incrementAndGet();
				return JacksonJsonHandlers.value().fromJson( text );
			}
		};
		var codec = new ElasticsearchJsonHandlerFieldCodec<>( json, custom );
		assertThat( codec.encode( null ) ).isSameAs( NullNode.instance );
		assertThat( codec.decode( (JsonNode) null ) ).isNull();
		assertThat( codec.decode( NullNode.instance ) ).isNull();
		assertThat( codec.decode( MissingNode.getInstance() ) ).isNull();
		assertThat( imports.get() + exports.get() ).isZero();
		JsonNode negativeZero = codec.decode( -0.0d );
		assertThat( Double.doubleToRawLongBits( negativeZero.doubleValue() ) ).isEqualTo( Long.MIN_VALUE );
		assertThat( exports.get() ).isOne();
		assertThatThrownBy( () -> codec.decode( Double.NaN ) ).isInstanceOf( SearchException.class );
	}

	@Test
	void compatibleCodecsRequireIdenticalHandlerTokenAndExistingConfiguration() {
		var codec = new ElasticsearchJsonHandlerFieldCodec<>( json, JacksonJsonHandlers.value() );
		assertThat( codec.isCompatibleWith( new ElasticsearchJsonHandlerFieldCodec<>( json, JacksonJsonHandlers.value() ) ) )
				.isTrue();
		assertThat( codec.isCompatibleWith( new ElasticsearchJsonHandlerFieldCodec<>( json, JacksonJsonHandlers.object() ) ) )
				.isFalse();
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
				return JacksonJsonHandlers.value().fromJson( text );
			}
		};
		assertThat( codec.isCompatibleWith( new ElasticsearchJsonHandlerFieldCodec<>( json, custom ) ) ).isFalse();
		assertThat( codec.isCompatibleWith( new ElasticsearchJsonHandlerFieldCodec<>(
				JsonProviderHelper.create( false ).getCodec(), JacksonJsonHandlers.value() ) ) ).isFalse();
	}

	@Test
	@SuppressWarnings("deprecation")
	void deprecatedAndTypedNativeFieldsShareSplittingDefaultAndAtomicModeIsExplicit() {
		var legacy = new ElasticsearchJsonHandlerFieldCodec<>( json, GsonJsonHandlers.value() );
		var typed = new ElasticsearchJsonHandlerFieldCodec<>( json, GsonJsonHandlers.value() );
		assertThat( legacy.canDecodeArrays() ).isFalse();
		assertThat( typed.canDecodeArrays() ).isFalse();
		var atomic = new ElasticsearchJsonHandlerFieldCodec<>( json, GsonJsonHandlers.value(), true );
		assertThat( atomic.canDecodeArrays() ).isTrue();
		assertThat( atomic.isCompatibleWith( typed ) ).isFalse();
		assertThat( typed.isCompatibleWith( atomic ) ).isFalse();
		assertThat( legacy.isCompatibleWith( new ElasticsearchJsonHandlerFieldCodec<>( json, GsonJsonHandlers.value() ) ) )
				.isTrue();
		assertThat( legacy.isCompatibleWith( typed ) ).isTrue();
		assertThat( typed.isCompatibleWith( legacy ) ).isTrue();
		JsonNode array = JacksonJsonHandlers.value().fromJson( "[1,null,{\"nested\":null}]" );
		assertThat( typed.decode( array ).isJsonArray() ).isTrue();
		assertThat( legacy.decode( array.get( 0 ) ).getAsInt() ).isEqualTo( 1 );
		assertThat( legacy.decode( array.get( 1 ) ) ).isNull();
	}

}
