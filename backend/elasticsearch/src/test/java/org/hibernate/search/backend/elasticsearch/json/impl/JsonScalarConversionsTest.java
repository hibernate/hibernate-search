package org.hibernate.search.backend.elasticsearch.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.node.StringNode;

class JsonScalarConversionsTest {
	@Test
	void numericNarrowingWrapsAndTruncatesWhileTextRetainsStrictParsing() {
		var parsing = new JsonParsing();
		for ( String text : new String[] {
				"4294967297",
				"18446744073709551617",
				"-18446744073709551617",
				"1.9",
				"-1.9",
				"1e20",
				"1e-20" } ) {
			var value = parsing.parse( text );
			var reference = new BigDecimal( text );
			assertThat( JsonScalarConversions.asInteger( value ) ).isEqualTo( reference.intValue() );
			assertThat( JsonScalarConversions.asLong( value ) ).isEqualTo( reference.longValue() );
			assertThat( JsonScalarConversions.asByte( value ) ).isEqualTo( reference.byteValue() );
			assertThat( JsonScalarConversions.asShort( value ) ).isEqualTo( reference.shortValue() );
		}
		assertThat( JsonScalarConversions.asLong( parsing.parse( "1e2147483647" ) ) ).isZero();
		assertThatThrownBy( () -> JsonScalarConversions.asInteger( StringNode.valueOf( "4294967297" ) ) )
				.isInstanceOf( NumberFormatException.class );
		assertThatThrownBy( () -> JsonScalarConversions.asLong( StringNode.valueOf( "1.9" ) ) )
				.isInstanceOf( NumberFormatException.class );
	}

	@Test
	void bigIntegerUsesIntegralValueRatherThanNumericSpelling() {
		var parsing = new JsonParsing();
		for ( String text : new String[] { "1000", "1e3", "1000.000" } ) {
			assertThat( JsonScalarConversions.asBigInteger( parsing.parse( text ) ) ).isEqualTo( new BigInteger( "1000" ) );
		}
		assertThatThrownBy( () -> JsonScalarConversions.asBigInteger( parsing.parse( "1.9" ) ) )
				.isInstanceOf( ArithmeticException.class );
		assertThatThrownBy( () -> JsonScalarConversions.asBigInteger( StringNode.valueOf( "1e3" ) ) )
				.isInstanceOf( NumberFormatException.class );
		assertThatThrownBy( () -> JsonScalarConversions.asBigInteger( parsing.parse( "1e100001" ) ) )
				.isInstanceOf( ArithmeticException.class );
	}

	@Test
	void mathematicalEqualityCrossesNumericNodeClassesWithoutDoubleRounding() {
		var parsing = new JsonParsing();
		assertThat( JsonScalarConversions.equivalent( parsing.parse( "{\"a\":[1]}" ), parsing.parse( "{\"a\":[1.0]}" ) ) )
				.isTrue();
		assertThat(
				JsonScalarConversions.equivalent( parsing.parse( "9007199254740993" ), parsing.parse( "9007199254740992" ) ) )
				.isFalse();
		assertThat( JsonScalarConversions.equivalent( parsing.parse( "-0.0" ), parsing.parse( "0" ) ) ).isTrue();
		assertThatThrownBy( () -> JsonScalarConversions.toNode( new Object() ) ).isInstanceOf( IllegalArgumentException.class );
	}
}
