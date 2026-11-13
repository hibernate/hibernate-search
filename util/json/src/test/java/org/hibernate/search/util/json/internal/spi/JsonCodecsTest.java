package org.hibernate.search.util.json.internal.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ObjectNode;

class JsonCodecsTest {
	private final JsonParsing parsing = new JsonParsing();

	@Test
	void recursiveCopyOmitsObjectNullsAndRetainsArrayPositions() {
		var source = parsing.parse( "{\"null\":null,\"nested\":{\"null\":null},\"array\":[null,{\"null\":null}]}" );
		var copy = (ObjectNode) JsonCodecs.copy( source, false );
		assertThat( copy ).isEqualTo( parsing.parse( "{\"nested\":{},\"array\":[null,{}]}" ) );
		assertThat( copy ).isNotSameAs( source );
		( (ObjectNode) copy.get( "array" ).get( 1 ) ).put( "changed", true );
		assertThat( source.get( "array" ).get( 1 ).has( "changed" ) ).isFalse();
		assertThat( JsonCodecs.copy( source, true ) ).isEqualTo( source ).isNotSameAs( source );
	}

	@Test
	void scalarsPreserveCoercionWithoutJacksonDefaultsOrDoubleRounding() {
		assertThat( JsonCodecs.LONG.decode( parsing.parse( "9007199254740993" ) ) ).isEqualTo( 9007199254740993L );
		assertThat( JsonCodecs.INTEGER.decode( parsing.parse( "1.9" ) ) ).isEqualTo( 1 );
		assertThat( JsonCodecs.INTEGER.decode( parsing.parse( "1e2147483647" ) ) ).isZero();
		assertThat( JsonCodecs.LONG.decode( parsing.parse( "1e2147483647" ) ) ).isZero();
		assertThat( JsonCodecs.INTEGER.decode( parsing.parse( "[\"42\"]" ) ) ).isEqualTo( 42 );
		assertThat( JsonCodecs.STRING.decode( parsing.parse( "[true]" ) ) ).isEqualTo( "true" );
		assertThat( JsonCodecs.BOOLEAN.decode( parsing.parse( "\"TrUe\"" ) ) ).isTrue();
		assertThat( JsonCodecs.BOOLEAN.decode( parsing.parse( "\"other\"" ) ) ).isFalse();
		assertThatThrownBy( () -> JsonCodecs.INTEGER.decode( parsing.parse( "\"1.0\"" ) ) )
				.isInstanceOf( NumberFormatException.class );
		assertThatThrownBy( () -> JsonCodecs.INTEGER.decode( parsing.parse( "\"2147483648\"" ) ) )
				.isInstanceOf( NumberFormatException.class );
		assertThatThrownBy( () -> JsonCodecs.BOOLEAN.decode( parsing.parse( "1" ) ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JsonCodecs.BOOLEAN.decode( parsing.parse( "[true]" ) ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JsonCodecs.DOUBLE.encode( Double.NaN, false ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void composedCodecsKeepNullsInListsAndApplyMapMemberPolicy() {
		var list = JsonCodecs.list( JsonCodecs.STRING );
		assertThat( list.encode( Arrays.asList( "a", null ), false ) )
				.isEqualTo( parsing.parse( "[\"a\",null]" ) );
		var map = new LinkedHashMap<String, String>();
		map.put( "a", "value" );
		map.put( "b", null );
		assertThat( JsonCodecs.map( JsonCodecs.STRING ).encode( map, false ) )
				.isEqualTo( parsing.parse( "{\"a\":\"value\"}" ) );
		assertThat( JsonCodecs.map( JsonCodecs.STRING ).encode( map, true ) )
				.isEqualTo( parsing.parse( "{\"a\":\"value\",\"b\":null}" ) );
	}
}
