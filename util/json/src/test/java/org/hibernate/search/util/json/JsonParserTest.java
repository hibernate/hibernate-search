package org.hibernate.search.util.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.NullNode;

class JsonParserTest {
	private final JsonParsing parsing = new JsonParsing();

	@ParameterizedTest
	@ValueSource(strings = {
			"null",
			"{\"long-ascii-property\":\"aaaaaaaaaaaaaaaaéaaaaaaaaaaaaaaaa😀\"}",
			"\"\\uD800\"",
			"{\"\\uD83D\\uDE00\":1}",
			"true",
			"false",
			"42",
			"-0",
			"1.2300e+04",
			"-1.25E-03",
			"9007199254740993",
			"0 ",
			"-0 ",
			"[0,-0,0 ]",
			"{\"z\":-0,\"zero\":0 }",
			"\"é中😀\\n\\uD83D\\uDE00\"",
			"{}",
			"[]",
			"{\"a\":null,\"a\":[1,{},[true]],\"b\":\"é\"} \r\n" })
	void allSplitPointsAndBufferKinds(String json) {
		byte[] bytes = json.getBytes( StandardCharsets.UTF_8 );
		JsonNode expected = parsing.parse( json );
		for ( int kind = 0; kind < 4; kind++ ) {
			for ( int split = 0; split <= bytes.length; split++ ) {
				try ( JsonParserSession parser = parsing.createParser() ) {
					consume( parser, buffer( bytes, 0, split, kind ) );
					consume( parser, buffer( bytes, split, bytes.length - split, kind ) );
					JsonNode actual = parser.endOfInput();
					assertThat( actual ).isEqualTo( expected );
					assertThat( actual.toString() ).isEqualTo( expected.toString() );
					assertThat( parser.inputBytes() ).isEqualTo( bytes.length );
				}
			}
		}
	}

	@Test
	void randomFragmentationOfLargeTokens() {
		String json = "{\"" + "é中😀".repeat( 200 ) + "\":\"" + "é中😀\\n\\u0041".repeat( 20_000 )
				+ "\",\"n\":123456789012345678901234567890.23000e+042}";
		byte[] bytes = json.getBytes( StandardCharsets.UTF_8 );
		JsonNode expected = parsing.parse( json );
		Random random = new Random( 357919 );
		try ( JsonParserSession parser = parsing.createParser() ) {
			for ( int offset = 0; offset < bytes.length; ) {
				int size = Math.min( 1 + random.nextInt( 5000 ), bytes.length - offset );
				consume( parser, buffer( bytes, offset, size, random.nextInt( 4 ) ) );
				offset += size;
			}
			assertThat( parser.endOfInput() ).isEqualTo( expected );
		}
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"",
			" \r\n",
			"{",
			"[1",
			"{\"a\":",
			"\"abc",
			"\"\\uD83",
			"1e",
			"1e+",
			"-",
			"tru",
			"nul",
			"{}{}",
			"null true",
			"42 43",
			"[]garbage",
			"{\"a\":1,}",
			"[1,]",
			"01",
			"NaN",
			"'text'",
			"/*x*/null",
			"{\"\\uD800\":1}",
			"{\"\\uDC00\":1}" })
	void rejectsEmptyMalformedTruncatedAndTrailingInput(String json) {
		assertThatThrownBy( () -> parsing.parse( json ) ).isInstanceOf( JsonParseException.class );
		JsonParserSession parser = parsing.createParser();
		assertThatThrownBy( () -> {
			consume( parser, ByteBuffer.wrap( json.getBytes( StandardCharsets.UTF_8 ) ) );
			parser.endOfInput();
		} ).isInstanceOf( JsonParseException.class );
		assertThatThrownBy( parser::endOfInput ).isInstanceOf( IllegalStateException.class );
		assertThatThrownBy( () -> parser.feed( ByteBuffer.allocate( 0 ) ) ).isInstanceOf( IllegalStateException.class );
		parser.close();
	}

	@Test
	void stringInputPreservesUtf8LimitsAndUnicodePolicy() {
		String json = "{\"😀\":0}";
		int bytes = json.getBytes( StandardCharsets.UTF_8 ).length;
		assertThat( new JsonParsing( JsonParsingOptions.DEFAULT.withMaxInputBytes( bytes ) ).parse( json ) )
				.isEqualTo( parsing.parse( json ) );
		assertThatThrownBy( () -> new JsonParsing( JsonParsingOptions.DEFAULT.withMaxInputBytes( bytes - 1 ) ).parse( json ) )
				.isInstanceOf( JsonParseException.class ).hasMessageContaining( "byte limit" );
		JsonParsing names = new JsonParsing( new JsonParsingOptions( 20, 100, 3, 100, 8192, 0 ) );
		assertThatThrownBy( () -> names.parse( json ) ).isInstanceOf( JsonParseException.class );
		assertThatThrownBy( () -> {
			try ( JsonParserSession parser = names.createParser() ) {
				consume( parser, ByteBuffer.wrap( json.getBytes( StandardCharsets.UTF_8 ) ) );
				parser.endOfInput();
			}
		} ).isInstanceOf( JsonParseException.class );
		for ( String broken : new String[] { "\"\ud800\"", "{\"\udc00\":0}" } ) {
			assertThatThrownBy( () -> parsing.parse( broken ) ).isInstanceOf( JsonParseException.class );
			JsonParsing byteLimited = new JsonParsing( JsonParsingOptions.DEFAULT.withMaxInputBytes( 100 ) );
			assertThatThrownBy( () -> byteLimited.parse( broken ) ).isInstanceOf( JsonParseException.class );
		}
	}

	@Test
	void rejectsMalformedAndTruncatedUtf8() {
		for ( byte[] bytes : new byte[][] {
				{ '"', (byte) 0xc3, '"' },
				{ '"', (byte) 0xf0, (byte) 0x9f },
				{ '"', (byte) 0xff, '"' },
				{ '"', (byte) 0xc0, (byte) 0xaf, '"' },
				{ '"', (byte) 0xed, (byte) 0xa0, (byte) 0x80, '"' },
				{ '"', (byte) 0xf4, (byte) 0x90, (byte) 0x80, (byte) 0x80, '"' } } ) {
			for ( int split = 0; split <= bytes.length; split++ ) {
				int boundary = split;
				assertThatThrownBy( () -> {
					try ( JsonParserSession parser = parsing.createParser() ) {
						consume( parser, buffer( bytes, 0, boundary, 2 ) );
						consume( parser, buffer( bytes, boundary, bytes.length - boundary, 3 ) );
						parser.endOfInput();
					}
				} ).isInstanceOf( JsonParseException.class );
			}
		}
	}

	@Test
	void longAsciiRunsDoNotMaskNonCanonicalUtf8() {
		for ( byte[] invalid : new byte[][] {
				{ (byte) 0xc0, (byte) 0xaf },
				{ (byte) 0xed, (byte) 0xa0, (byte) 0x80 },
				{ (byte) 0xf4, (byte) 0x90, (byte) 0x80, (byte) 0x80 },
				{ (byte) 0xe0, (byte) 0x80, (byte) 0xaf },
				{ (byte) 0xc3, 'a' },
				{ (byte) 0x80 } } ) {
			for ( int alignment = 0; alignment < 16; alignment++ ) {
				byte[] prefix = ( "\"" + "a".repeat( 16 + alignment ) ).getBytes( StandardCharsets.UTF_8 );
				byte[] bytes = new byte[prefix.length + invalid.length + 1];
				System.arraycopy( prefix, 0, bytes, 0, prefix.length );
				System.arraycopy( invalid, 0, bytes, prefix.length, invalid.length );
				bytes[bytes.length - 1] = '"';
				for ( int kind = 0; kind < 4; kind++ ) {
					int bufferKind = kind;
					for ( int split = prefix.length - 1; split < bytes.length; split++ ) {
						int boundary = split;
						assertThatThrownBy( () -> {
							try ( JsonParserSession parser = parsing.createParser() ) {
								consume( parser, buffer( bytes, 0, boundary, bufferKind ) );
								consume( parser, buffer( bytes, boundary, bytes.length - boundary, bufferKind ) );
								parser.endOfInput();
							}
						} ).isInstanceOf( JsonParseException.class );
					}
				}
			}
		}
	}

	@Test
	void completeRootStillRequiresEndAndRejectsLaterContent() {
		try ( JsonParserSession parser = parsing.createParser() ) {
			consume( parser, ByteBuffer.wrap( "{}".getBytes( StandardCharsets.UTF_8 ) ) );
			assertThatThrownBy( () -> consume( parser, ByteBuffer.wrap( " []".getBytes( StandardCharsets.UTF_8 ) ) ) )
					.isInstanceOf( JsonParseException.class );
		}
		JsonParserSession completed = parsing.createParser();
		consume( completed, ByteBuffer.wrap( "null".getBytes( StandardCharsets.UTF_8 ) ) );
		assertThat( completed.endOfInput() ).isSameAs( NullNode.instance );
		assertThatThrownBy( completed::endOfInput ).isInstanceOf( IllegalStateException.class );
	}

	@Test
	void boundedConsumptionAndReturnedBufferOwnership() {
		JsonParsing limited = new JsonParsing( new JsonParsingOptions( 20, 100, 100, 100, 3, 0 ) );
		ByteBuffer input = ByteBuffer.wrap( "{\"a\":\"value\"}".getBytes( StandardCharsets.UTF_8 ) );
		try ( JsonParserSession parser = limited.createParser() ) {
			parser.feed( ByteBuffer.allocate( 0 ) );
			assertThat( parser.inputBytes() ).isZero();
			parser.feed( input );
			assertThat( input.position() ).isEqualTo( 3 );
			assertThat( parser.inputBytes() ).isEqualTo( 3 );
			input.put( 0, (byte) '!' );
			input.put( 1, (byte) '!' );
			input.put( 2, (byte) '!' );
			consume( parser, input );
			assertThat( parser.endOfInput().get( "a" ).asString() ).isEqualTo( "value" );
		}
	}

	@Test
	void cancellationIsTerminalAndIdempotent() {
		JsonParserSession parser = parsing.createParser();
		consume( parser, ByteBuffer.wrap( "{\"a\":[1,".getBytes( StandardCharsets.UTF_8 ) ) );
		parser.close();
		parser.close();
		assertThatThrownBy( parser::endOfInput ).isInstanceOf( IllegalStateException.class );
		assertThatThrownBy( () -> parser.feed( ByteBuffer.wrap( new byte[] { '2' } ) ) )
				.isInstanceOf( IllegalStateException.class );
	}

	@Test
	void independentlyConfiguredConstraintsAndByteLimits() {
		JsonParsing constrained = new JsonParsing( new JsonParsingOptions( 2, 3, 3, 3, 2, 0 ) );
		for ( String json : new String[] { "[[[]]]", "\"abcd\"", "{\"abcd\":1}", "1234" } ) {
			assertThatThrownBy( () -> constrained.parse( json ) ).isInstanceOf( JsonParseException.class );
		}
		JsonParsing byteLimited = new JsonParsing( JsonParsingOptions.DEFAULT.withMaxInputBytes( 4 ) );
		assertThat( byteLimited.parse( "null" ) ).isSameAs( NullNode.instance );
		assertThatThrownBy( () -> byteLimited.parse( "false" ) ).isInstanceOf( JsonParseException.class )
				.hasMessageContaining( "byte limit" );
		assertThat( JsonParsingOptions.DEFAULT.maxInputBytes() ).isZero();
		assertThatThrownBy( () -> JsonParsingOptions.DEFAULT.withMaxInputBytes( -1 ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@ParameterizedTest
	@ValueSource(strings = { "UTF-16LE", "UTF-16BE", "UTF-16", "ISO-8859-1", "Shift_JIS", "ISO-2022-JP" })
	void explicitCharsetsAtAllByteBoundaries(String charsetName) {
		Charset charset = Charset.forName( charsetName );
		String json = charsetName.startsWith( "UTF" ) ? "{\"é中😀\":\"é中😀\"}" : "{\"value\":\"é日本\"}";
		byte[] bytes = json.getBytes( charset );
		JsonNode expected = parsing.parse( new String( bytes, charset ) );
		for ( int split = 0; split <= bytes.length; split++ ) {
			try ( JsonParserSession parser = parsing.createParser( charset ) ) {
				consume( parser, buffer( bytes, 0, split, 1 ) );
				consume( parser, buffer( bytes, split, bytes.length - split, 3 ) );
				assertThat( parser.endOfInput() ).isEqualTo( expected );
				assertThat( parser.inputBytes() ).isEqualTo( bytes.length );
			}
		}
	}

	@Test
	void charsetLimitsCountOriginalBytesAndBoundedBuffersHandleLargeInput() {
		String json = "\"é\"";
		byte[] bytes = json.getBytes( StandardCharsets.ISO_8859_1 );
		JsonParsing originalByteLimit = new JsonParsing( JsonParsingOptions.DEFAULT.withMaxInputBytes( bytes.length ) );
		try ( JsonParserSession parser = originalByteLimit.createParser( StandardCharsets.ISO_8859_1 ) ) {
			consume( parser, ByteBuffer.wrap( bytes ) );
			assertThat( parser.endOfInput().asString() ).isEqualTo( "é" );
		}
		assertThatThrownBy( () -> {
			try ( JsonParserSession parser = originalByteLimit.createParser( StandardCharsets.UTF_16LE ) ) {
				consume( parser, ByteBuffer.wrap( json.getBytes( StandardCharsets.UTF_16LE ) ) );
			}
		} ).isInstanceOf( JsonParseException.class ).hasMessageContaining( "byte limit" );
		String large = "\"" + "é中😀".repeat( 50_000 ) + "\"";
		try ( JsonParserSession parser = parsing.createParser( StandardCharsets.UTF_16LE ) ) {
			ByteBuffer input = ByteBuffer.wrap( large.getBytes( StandardCharsets.UTF_16LE ) );
			while ( input.hasRemaining() ) {
				int before = input.position();
				parser.feed( input );
				assertThat( input.position() - before ).isBetween( 1, 8192 );
			}
			assertThat( parser.endOfInput() ).isEqualTo( parsing.parse( large ) );
		}
	}

	@Test
	void charsetTruncationAndCancellation() {
		JsonParserSession parser = parsing.createParser( StandardCharsets.UTF_16LE );
		consume( parser, ByteBuffer.wrap( new byte[] { '"', 0, 'a' } ) );
		assertThatThrownBy( parser::endOfInput ).isInstanceOf( JsonParseException.class );
		assertThatThrownBy( parser::endOfInput ).isInstanceOf( IllegalStateException.class );
		JsonParserSession cancelled = parsing.createParser( StandardCharsets.UTF_16LE );
		cancelled.close();
		assertThatThrownBy( () -> cancelled.feed( ByteBuffer.allocate( 0 ) ) ).isInstanceOf( IllegalStateException.class );
	}

	private static void consume(JsonParserSession parser, ByteBuffer input) {
		while ( input.hasRemaining() ) {
			parser.feed( input );
		}
	}

	private static ByteBuffer buffer(byte[] bytes, int offset, int size, int kind) {
		ByteBuffer buffer = kind == 1 ? ByteBuffer.allocateDirect( size + 4 ) : ByteBuffer.allocate( size + 4 );
		buffer.position( 2 );
		buffer.put( bytes, offset, size );
		buffer.limit( size + 2 );
		buffer.position( 2 );
		if ( kind == 2 ) {
			return buffer.slice();
		}
		if ( kind == 3 ) {
			return buffer.asReadOnlyBuffer();
		}
		return buffer;
	}
}
