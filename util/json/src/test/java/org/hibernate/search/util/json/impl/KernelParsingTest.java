package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.LongNode;

class KernelParsingTest {

	private final JsonParsing parsing = new JsonParsing();

	@Test
	void exactTypesNumbersAndFloatingZeroAgreeAtEverySplit() {
		String text =
				"{\"escaped\\uD83D\\uDE00\":[-0,-0.0,-0e3,0,42,2147483648,9007199254740993,9223372036854775808,0.12345678901234567890123456789],\"text\":\"é漢😀\",\"a\":true,\"b\":null}";
		JsonNode expected = parsing.parse( text );
		var numbers = expected.get( "escaped😀" );
		assertThat( numbers.get( 0 ) ).isInstanceOf( IntNode.class );
		assertThat( numbers.get( 1 ) ).isInstanceOf( DoubleNode.class );
		assertThat( Double.doubleToRawLongBits( numbers.get( 1 ).doubleValue() ) )
				.isEqualTo( Double.doubleToRawLongBits( -0.0d ) );
		assertThat( Double.doubleToRawLongBits( numbers.get( 2 ).doubleValue() ) )
				.isEqualTo( Double.doubleToRawLongBits( -0.0d ) );
		assertThat( numbers.get( 4 ) ).isInstanceOf( IntNode.class );
		assertThat( numbers.get( 5 ) ).isInstanceOf( LongNode.class );
		assertThat( numbers.get( 6 ).longValue() ).isEqualTo( 9007199254740993L );
		assertThat( numbers.get( 7 ).bigIntegerValue() ).isEqualTo( new BigInteger( "9223372036854775808" ) );
		assertThat( numbers.get( 8 ).decimalValue() )
				.isEqualByComparingTo( new BigDecimal( "0.12345678901234567890123456789" ) );
		byte[] bytes = text.getBytes( StandardCharsets.UTF_8 );
		for ( int split = 0; split <= bytes.length; split++ ) {
			try ( var session = parsing.createParser() ) {
				feedAll( session, ByteBuffer.wrap( bytes, 0, split ) );
				feedAll( session, ByteBuffer.wrap( bytes, split, bytes.length - split ) );
				assertThat( session.endOfInput() ).as( "split %s", split ).isEqualTo( expected );
				assertThat( session.inputBytes() ).isEqualTo( bytes.length );
			}
		}
		try ( var session = parsing.createParser() ) {
			for ( byte value : bytes ) {
				session.feed( ByteBuffer.wrap( new byte[] { value } ) );
			}
			assertThat( session.endOfInput() ).isEqualTo( expected );
		}
	}

	@Test
	void numericRangeRejectionIsIdenticalAcrossTextAndBytes() {
		for ( String text : new String[] {
				"1e2147483648",
				"1e-2147483648",
				"10e2147483647",
				"0e2147483648",
				"1".repeat( 1001 ) } ) {
			assertThatThrownBy( () -> parsing.parse( text ) ).isInstanceOf( JsonParseException.class );
			assertThatThrownBy( () -> {
				try ( var session = parsing.createParser() ) {
					feedAll( session, ByteBuffer.wrap( text.getBytes( StandardCharsets.UTF_8 ) ) );
					session.endOfInput();
				}
			} ).isInstanceOf( JsonParseException.class );
		}
		for ( String text : new String[] { "1e2147483647", "1e-2147483647", "-0.0", "-0e5", "1.00", "-9223372036854775808" } ) {
			JsonNode node = parsing.parse( text );
			assertThat( parsing.parse( JsonNumbers.render( node ) ) ).isEqualTo( node );
		}
	}

	@Test
	void boundedFeedsAdvancePositionAndLimitsCountOriginalTranscodedBytes() {
		var options = new JsonParsingOptions( 1000, 20000000, 50000, 1000, 3, 0 );
		byte[] bytes = "{\"text\":\"é😀\"}".getBytes( StandardCharsets.UTF_8 );
		try ( var session = new JsonParsing( options ).createParser() ) {
			ByteBuffer input = ByteBuffer.wrap( bytes );
			while ( input.hasRemaining() ) {
				int previous = input.position();
				session.feed( input );
				assertThat( input.position() - previous ).isEqualTo( Math.min( 3, bytes.length - previous ) );
			}
			assertThat( session.endOfInput() ).isEqualTo( parsing.parse( "{\"text\":\"é😀\"}" ) );
		}
		byte[] utf16 = "{\"text\":\"é😀\"}".getBytes( StandardCharsets.UTF_16LE );
		var limited = new JsonParsing( options.withMaxInputBytes( utf16.length ) );
		try ( var session = limited.createParser( StandardCharsets.UTF_16LE ) ) {
			feedAll( session, ByteBuffer.wrap( utf16 ) );
			assertThat( session.endOfInput() ).isEqualTo( parsing.parse( "{\"text\":\"é😀\"}" ) );
			assertThat( session.inputBytes() ).isEqualTo( utf16.length );
		}
		assertThatThrownBy( () -> {
			try ( var session = new JsonParsing( options.withMaxInputBytes( utf16.length - 1 ) )
					.createParser( StandardCharsets.UTF_16LE ) ) {
				feedAll( session, ByteBuffer.wrap( utf16 ) );
			}
		} ).isInstanceOf( JsonParseException.class );
	}

	@Test
	void strictRootsUtf8AndCharacterConstraints() {
		for ( String text : new String[] { "", " ", "{}{}", "[1,", "{\"x\":}", "{'x':1}" } ) {
			assertThatThrownBy( () -> parsing.parse( text ) ).isInstanceOf( JsonParseException.class );
		}
		for ( byte[] bytes : new byte[][] {
				{ '"', (byte) 0xc0, (byte) 0x80, '"' },
				{ '"', (byte) 0xed, (byte) 0xa0, (byte) 0x80, '"' },
				{ '"', (byte) 0xf0 } } ) {
			assertThatThrownBy( () -> {
				try ( var session = parsing.createParser() ) {
					feedAll( session, ByteBuffer.wrap( bytes ) );
					session.endOfInput();
				}
			} ).isInstanceOf( JsonParseException.class );
		}
		assertThatThrownBy( () -> parsing.parse( "\"" + (char) 0xd800 + "\"" ) ).isInstanceOf( JsonParseException.class );
		var shortNames = new JsonParsing( new JsonParsingOptions( 3, 20, 3, 10, 8192, 0 ) );
		assertThatThrownBy( () -> shortNames.parse( "{\"éé\":1}" ) ).isInstanceOf( JsonParseException.class );
		assertThat( parsing.parse( "{\"duplicate\":1,\"duplicate\":2}" ).get( "duplicate" ).intValue() ).isEqualTo( 2 );
	}

	static void feedAll(JsonParserSession session, ByteBuffer input) {
		while ( input.hasRemaining() ) {
			session.feed( input );
		}
	}
}
