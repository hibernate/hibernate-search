package org.hibernate.search.backend.elasticsearch.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.ObjectNode;

class JsonAccessorNumericPolicyTest {
	private static final JsonAccessor<Long> STRICT = JsonAccessor.root().property( "value" ).asLong();
	private static final JsonAccessor<Long> ADAPTED = JsonAccessor.root().property( "value" ).unwrapSingletonArrays().asLong();

	@Test
	void typedNumericReadsPreserveTruncationWrappingAndBoundedHugeScaleConversion() {
		String[] values = { "1.9", "-1.9", "18446744073709551617", "1e2147483647", "1e-2147483647" };
		long[] expected = { 1, -1, 1, 0, 0 };
		for ( int i = 0; i < values.length; i++ ) {
			assertThat( STRICT.get( tree( values[i] ) ) ).contains( expected[i] );
			assertThat( ADAPTED.get( tree( "[[" + values[i] + "]]" ) ) ).contains( expected[i] );
		}
	}

	@Test
	void numericTypeSelectionStaysStrictAndArrayAdaptationRemainsExplicit() {
		for ( String value : new String[] { "\"42\"", "true", "{}", "[42]" } ) {
			assertThatThrownBy( () -> STRICT.get( tree( value ) ) )
					.isInstanceOf( UnexpectedJsonElementTypeException.class );
		}
		assertThat( STRICT.get( tree( "null" ) ) ).isEmpty();
		assertThat( ADAPTED.get( tree( "[]" ) ) ).isEmpty();
		assertThat( ADAPTED.get( tree( "[[null]]" ) ) ).isEmpty();
		assertThatThrownBy( () -> ADAPTED.get( tree( "[1,2]" ) ) ).isInstanceOf( IllegalStateException.class );
	}

	private static ObjectNode tree(String value) {
		return (ObjectNode) new JsonParsing().parse( "{\"value\":" + value + "}" );
	}
}
