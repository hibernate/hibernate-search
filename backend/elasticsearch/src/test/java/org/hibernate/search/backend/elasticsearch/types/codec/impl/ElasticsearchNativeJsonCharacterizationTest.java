package org.hibernate.search.backend.elasticsearch.types.codec.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

class ElasticsearchNativeJsonCharacterizationTest {

	@Test
	void nativeValuesShareTreeIdentityAndMapNulls() {
		ElasticsearchJsonHandlerFieldCodec<JsonNode> codec = new ElasticsearchJsonHandlerFieldCodec<>(
				JsonProviderHelper.create( false ).getCodec(),
				JacksonJsonHandlers.value() );
		JsonNode value = new JsonParsing().parse( "{\"native\":[1,9007199254740993,null,{\"text\":\"é漢😀\"}]}" );
		assertThat( codec.encode( value ) ).isSameAs( value );
		assertThat( codec.decode( value ) ).isSameAs( value );
		assertThat( codec.encode( null ) ).isSameAs( NullNode.instance );
		assertThat( codec.decode( (JsonNode) null ) ).isNull();
		assertThat( codec.decode( NullNode.instance ) ).isNull();
		JsonNode reparsed = codec.fromJsonStringToElement( codec.fromJsonElementToString( value ) );
		assertThat( reparsed ).isEqualTo( value ).isNotSameAs( value );
		assertThat( reparsed.get( "native" ).get( 1 ).longValue() )
				.isEqualTo( 9007199254740993L );
	}
}
