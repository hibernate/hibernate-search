package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.COLLECTION;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;

class JsonParserCleanupTest {
	@Test
	void utf8SessionsReleasePartialTreesAndParserReferencesOnEveryTerminalPath() {
		JsonParsing parsing = new JsonParsing();
		JsonParserSession cancelled = parsing.createParser();
		cancelled.feed( ByteBuffer.wrap( "{\"a\":[1,".getBytes( StandardCharsets.UTF_8 ) ) );
		assertThat( cancelled ).extracting( "builder" ).extracting( "root" )
				.isEqualTo( parsing.parse( "{\"a\":[1]}" ) );
		assertThat( cancelled ).extracting( "parser" ).extracting( "_inputBuffer" )
				.satisfies( value -> assertThat( ( (ByteBuffer) value ).capacity() ).isZero() );
		assertThat( cancelled ).extracting( "parser" ).extracting( "inputArray" ).isNull();
		cancelled.close();
		assertUtf8Clean( cancelled );
		JsonParserSession failed = parsing.createParser();
		failed.feed( ByteBuffer.wrap( "{\"a\":[1,".getBytes( StandardCharsets.UTF_8 ) ) );
		assertThatThrownBy( failed::endOfInput ).isInstanceOf( JsonParseException.class );
		assertUtf8Clean( failed );
		JsonParserSession completed = parsing.createParser();
		completed.feed( ByteBuffer.wrap( "{\"a\":[1]}".getBytes( StandardCharsets.UTF_8 ) ) );
		assertThat( completed.endOfInput().get( "a" ) ).hasSize( 1 );
		assertUtf8Clean( completed );
	}

	@Test
	void mixedBufferKindsReturnCallerOwnershipAfterEveryFeed() {
		JsonParsing parsing = new JsonParsing();
		String json = "{\"\\uD83D\\uDE00\":[-0,9223372036854775808,1.00,-7E-9,1e2147483647],\"text\":\"aaaaaaaaaaaaaaaaé漢😀\"}";
		byte[] bytes = json.getBytes( StandardCharsets.UTF_8 );
		try ( JsonParserSession parser = parsing.createParser() ) {
			for ( int offset = 0, kind = 0; offset < bytes.length; kind++ ) {
				int count = Math.min( 3, bytes.length - offset );
				ByteBuffer storage = kind % 3 == 1 ? ByteBuffer.allocateDirect( count + 4 ) : ByteBuffer.allocate( count + 4 );
				storage.position( 2 );
				storage.put( bytes, offset, count ).flip().position( 2 );
				ByteBuffer offered = kind % 3 == 2 ? storage.asReadOnlyBuffer() : storage.slice();
				parser.feed( offered );
				assertThat( offered.hasRemaining() ).isFalse();
				assertThat( parser ).extracting( "parser" ).extracting( "inputArray" ).isNull();
				assertThat( parser ).extracting( "parser" ).extracting( "_inputBuffer" )
						.satisfies( value -> assertThat( ( (ByteBuffer) value ).capacity() ).isZero() );
				for ( int i = 2; i < storage.limit(); i++ ) {
					storage.put( i, (byte) '!' );
				}
				offset += count;
			}
			assertThat( parser.endOfInput().toString() ).isEqualTo( parsing.parse( json ).toString() );
		}
	}

	@Test
	void transcodingSessionsReleaseAllBuffersOnCancellationFailureAndCompletion() {
		JsonParsing parsing = new JsonParsing();
		JsonParserSession cancelled = parsing.createParser( StandardCharsets.UTF_16LE );
		cancelled.feed( ByteBuffer.wrap( "{\"a\":[1,".getBytes( StandardCharsets.UTF_16LE ) ) );
		cancelled.close();
		assertTranscoderClean( cancelled );
		JsonParserSession failed = parsing.createParser( StandardCharsets.UTF_16LE );
		failed.feed( ByteBuffer.wrap( new byte[] { '"', 0, 'a' } ) );
		assertThatThrownBy( failed::endOfInput ).isInstanceOf( JsonParseException.class );
		assertTranscoderClean( failed );
		JsonParserSession completed = parsing.createParser( StandardCharsets.UTF_16LE );
		completed.feed( ByteBuffer.wrap( "{}".getBytes( StandardCharsets.UTF_16LE ) ) );
		completed.endOfInput();
		assertTranscoderClean( completed );
	}

	private static void assertUtf8Clean(JsonParserSession parser) {
		assertThat( parser ).extracting( "parser", "feeder" ).containsExactly( null, null );
		assertThat( parser ).extracting( "builder" ).extracting( "root" ).isNull();
		assertThat( parser ).extracting( "builder" ).extracting( "frames" ).asInstanceOf( COLLECTION ).isEmpty();
	}

	private static void assertTranscoderClean(JsonParserSession parser) {
		assertThat( parser ).extracting( "delegate", "decoder", "encoder", "pending", "chars", "utf8" )
				.containsExactly( null, null, null, null, null, null );
	}
}
