package org.hibernate.search.util.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.json.JsonTestNodes.scalar;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.json.impl.JacksonJsonBodyWriter;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonWriteStrategy;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class JsonOutputTest {
	@Test
	void defaultBlockingOutputSelectsNativeWriter() throws IOException {
		JsonBody customNative = new org.hibernate.search.util.json.internal.spi.InternalJsonBody() {
			@Override
			public JsonWriteCursor openCursor() {
				throw new AssertionFailure( "The default blocking write must select the native writer" );
			}

			@Override
			public void writeTo(OutputStream destination, JsonWriteStrategy strategy) throws IOException {
				if ( strategy == JsonWriteStrategy.NATIVE ) {
					destination.write( "{}".getBytes( StandardCharsets.UTF_8 ) );
				}
				else {
					org.hibernate.search.util.json.internal.spi.InternalJsonBody.super.writeTo( destination, strategy );
				}
			}
		};
		var output = new ByteArrayOutputStream();
		customNative.writeTo( output );
		assertThat( output.toString( StandardCharsets.UTF_8 ) ).isEqualTo( "{}" );
	}

	@Test
	void strategiesPreserveFramingAndCustomBodyFallback() throws IOException {
		for ( JsonBody body : List.of(
				JsonBodies.ndjson( List.of() ),
				JsonBodies.ndjson( List.of( fixture(), new JsonParsing().parse( "-1.2300e+04" ) ) ),
				JsonBodies.withContentLengthProbe( JsonBodies.of( fixture() ), 0 ) ) ) {
			synchronous( body );
		}
		JsonBody custom = () -> JsonBodies.of( fixture() ).openCursor();
		synchronous( custom );
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3, 4, 5, 6, 7, 11, 8192 })
	void fragmentedOutputAndBufferBoundaries(int capacity) throws IOException {
		ObjectNode tree = fixture();
		JsonBody body = JsonBodies.of( tree );
		byte[] expected = jackson( tree );
		assertThat( synchronous( body ) ).isEqualTo( expected );
		for ( int kind = 0; kind < 3; kind++ ) {
			ByteBuffer storage = kind == 1
					? ByteBuffer.allocateDirect( capacity + 7 )
					: ByteBuffer.allocate( capacity + 7 );
			for ( int i = 0; i < storage.capacity(); i++ ) {
				storage.put( i, (byte) 0x7f );
			}
			ByteBuffer destination = storage;
			if ( kind == 2 ) {
				storage.position( 2 ).limit( capacity + 5 );
				destination = storage.slice();
			}
			ByteArrayOutputStream actual = new ByteArrayOutputStream();
			try ( JsonWriteCursor cursor = body.openCursor() ) {
				assertThat( cursor.write( ByteBuffer.allocate( 0 ) ) ).isZero();
				assertThat( cursor.isComplete() ).isFalse();
				while ( !cursor.isComplete() ) {
					destination.position( 1 ).limit( capacity + 1 );
					int count = cursor.write( destination );
					assertThat( count ).isBetween( 1, capacity );
					assertThat( destination.position() ).isEqualTo( count + 1 );
					assertThat( destination.limit() ).isEqualTo( capacity + 1 );
					destination.flip().position( 1 );
					while ( destination.hasRemaining() ) {
						actual.write( destination.get() );
					}
				}
				assertThat( cursor.write( ByteBuffer.allocate( 1 ) ) ).isZero();
				assertThat( cursor.write( ByteBuffer.allocate( 0 ) ) ).isZero();
			}
			assertThat( actual.toByteArray() ).isEqualTo( expected );
			assertThat( storage.get( kind == 2 ? 2 : 0 ) ).isEqualTo( (byte) 0x7f );
			storage.clear();
			assertThat( storage.get( capacity + 6 ) ).isEqualTo( (byte) 0x7f );
		}
	}

	@Test
	void allCharacterEscapesAndSupplementaryPairsAgreeWithJackson() throws IOException {
		StringBuilder value = new StringBuilder();
		for ( int c = 0; c <= 0xffff; c++ ) {
			value.append( (char) c );
		}
		value.append( "😀\ud800x\udfff\ud800\ud800\udfff" );
		ObjectNode tree = new ObjectNode( JsonNodeFactory.instance );
		tree.put( value.toString(), value.toString() );
		assertThat( synchronous( JsonBodies.of( tree ) ) ).isEqualTo( jackson( tree ) );
		JsonNode exact = scalar( "<>&='\u2028\u2029é中😀\ud800x\udfff" );
		assertThat( new String( synchronous( JsonBodies.of( exact ) ), StandardCharsets.UTF_8 ) )
				.isEqualTo( "\"\\u003c\\u003e\\u0026\\u003d\\u0027\\u2028\\u2029é中😀\\ud800x\\udfff\"" );
	}

	@Test
	void directUnicodeRunsYieldAtEveryEncodedUnitBoundary() throws IOException {
		String text = "aé中😀\"\\\b\f\n\r\t\u0000<>&='\u2028\u2029\ud800x\udfff";
		ObjectNode tree = new ObjectNode( JsonNodeFactory.instance );
		// Vary the prefix to exercise each remaining destination size for both names and values.
		for ( int prefix = 0; prefix < 12; prefix++ ) {
			tree.put( "x".repeat( prefix ) + text, text.repeat( 4 ) );
		}
		byte[] expected = jackson( tree );
		for ( int capacity = 1; capacity <= 32; capacity++ ) {
			assertThat( drain( JsonBodies.of( tree ), capacity ) ).isEqualTo( expected );
		}
	}

	@Test
	void stableBoundedContentLengthAndExactLimit() throws IOException {
		for ( int length : new int[] { 1, 8191, 8192, 8193, 1_000_000 } ) {
			JsonNode tree = length == 1 ? scalar( 0 ) : scalar( "a".repeat( length - 2 ) );
			JsonBody body = JsonBodies.of( tree );
			assertThat( body.contentLength() ).isEqualTo( length <= 8192 ? length : -1 );
			body.writeTo( OutputStream.nullOutputStream() );
			assertThat( body.contentLength() ).isEqualTo( length <= 8192 ? length : -1 );
		}
		JsonBody body = JsonBodies.of( scalar( "abc" ) );
		assertThat( JsonBodies.withContentLengthProbe( body, 0 ).contentLength() ).isEqualTo( -1 );
		assertThat( JsonBodies.withContentLengthProbe( body, 4 ).contentLength() ).isEqualTo( -1 );
		assertThat( JsonBodies.withContentLengthProbe( body, 5 ).contentLength() ).isEqualTo( 5 );
		assertThatThrownBy( () -> JsonBodies.withContentLengthProbe( body, -1 ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@Test
	void independentConcurrentCursorsAndRetryAfterPartialOutput() throws Exception {
		JsonBody body = JsonBodies.of( fixture() );
		byte[] expected = synchronous( body );
		try ( JsonWriteCursor partial = body.openCursor() ) {
			partial.write( ByteBuffer.allocate( 7 ) );
			assertThat( partial.isComplete() ).isFalse();
			CompletableFuture<byte[]> first = CompletableFuture.supplyAsync( () -> drain( body, 1 ) );
			CompletableFuture<byte[]> second = CompletableFuture.supplyAsync( () -> drain( body, 5 ) );
			assertThat( first.get() ).isEqualTo( expected );
			assertThat( second.get() ).isEqualTo( expected );
			partial.cancel();
			assertThatThrownBy( () -> partial.write( ByteBuffer.allocate( 1 ) ) ).isInstanceOf( IllegalStateException.class );
		}
		assertThat( drain( body, 2 ) ).isEqualTo( expected );
	}

	@Test
	void readOnlyRejectionDoesNotConsumeAndOutputStorageIsCallerOwned() {
		JsonBody body = JsonBodies.of( fixture() );
		try ( JsonWriteCursor cursor = body.openCursor() ) {
			assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 5 ).asReadOnlyBuffer() ) )
					.isInstanceOf( ReadOnlyBufferException.class );
			ByteBuffer first = ByteBuffer.allocate( 5 );
			cursor.write( first );
			byte[] retained = first.array().clone();
			while ( !cursor.isComplete() ) {
				cursor.write( ByteBuffer.allocate( 2 ) );
			}
			assertThat( first.array() ).isEqualTo( retained );
			assertThat( retained ).isEqualTo( Arrays.copyOf( drain( body, 5 ), 5 ) );
		}
	}

	@Test
	void signingStreamsReplayAndCloseIndependently() throws IOException {
		JsonBody body = JsonBodies.of( fixture() );
		byte[] expected = synchronous( body );
		try ( InputStream first = body.openStream(); InputStream second = body.openStream() ) {
			assertThat( first.read() ).isEqualTo( expected[0] & 0xff );
			assertThat( first.read( new byte[1], 0, 0 ) ).isZero();
			assertThat( second.readAllBytes() ).isEqualTo( expected );
			assertThat( first.readAllBytes() ).isEqualTo( Arrays.copyOfRange( expected, 1, expected.length ) );
			assertThat( first.read() ).isEqualTo( -1 );
		}
		InputStream cancelled = body.openStream();
		cancelled.close();
		cancelled.close();
		assertThatThrownBy( cancelled::read ).isInstanceOf( IOException.class );
		assertThat( synchronous( body ) ).isEqualTo( expected );
	}

	@Test
	void synchronousAdaptersDoNotCloseOrFlushDestinationAndPropagateFailure() throws IOException {
		OutputStream output = new OutputStream() {
			@Override
			public void write(int value) {
			}

			@Override
			public void flush() throws IOException {
				throw new IOException( "flush" );
			}

			@Override
			public void close() throws IOException {
				throw new IOException( "close" );
			}
		};
		JsonBodies.of( fixture() ).writeTo( output );
		JsonBodies.ndjson( List.of( fixture(), fixture() ) ).writeTo( output );
		for ( JsonWriteStrategy strategy : JsonWriteStrategy.values() ) {
			org.hibernate.search.util.json.internal.spi.JsonWriting.writeTo( JsonBodies.of( fixture() ), output, strategy );
			org.hibernate.search.util.json.internal.spi.JsonWriting
					.writeTo( JsonBodies.ndjson( List.of( fixture(), fixture() ) ), output, strategy );
		}
		OutputStream failure = new OutputStream() {
			@Override
			public void write(int value) throws IOException {
				throw new IOException( "sink failure" );
			}
		};
		assertThatThrownBy( () -> JsonBodies.of( fixture() ).writeTo( failure ) )
				.isInstanceOf( IOException.class ).hasMessage( "sink failure" );
		assertThatThrownBy( () -> JsonBodies.ndjson( List.of( fixture(), fixture() ) ).writeTo( failure ) )
				.isInstanceOf( IOException.class ).hasMessage( "sink failure" );
		for ( JsonWriteStrategy strategy : JsonWriteStrategy.values() ) {
			assertThatThrownBy( () -> org.hibernate.search.util.json.internal.spi.JsonWriting
					.writeTo( JsonBodies.of( fixture() ), failure, strategy ) )
					.isInstanceOf( IOException.class ).hasMessage( "sink failure" );
		}
	}

	@Test
	void exactScalarValuesAndEmptyContainers() throws IOException {
		for ( String lexeme : new String[] { "-0", "0.000", "1.2300e+04", "1.0E-04", "9007199254740993", "-1e+0003" } ) {
			assertThat( new String(
					drain( JsonBodies.of( new JsonParsing().parse( lexeme ) ), 1 ),
					StandardCharsets.UTF_8 ) )
					.isEqualTo( new String( jackson( new JsonParsing().parse( lexeme ) ),
							StandardCharsets.UTF_8 ) );
		}
		ArrayNode tree = new ArrayNode( JsonNodeFactory.instance );
		tree.add( new ObjectNode( JsonNodeFactory.instance ) );
		tree.add( new ArrayNode( JsonNodeFactory.instance ) );
		tree.add( "" );
		tree.add( true );
		tree.add( false );
		tree.add( (JsonNode) null );
		assertThat( new String( synchronous( JsonBodies.of( tree ) ), StandardCharsets.UTF_8 ) )
				.isEqualTo( "[{},[],\"\",true,false,null]" );
	}

	static ObjectNode fixture() {
		ObjectNode tree = new ObjectNode( JsonNodeFactory.instance );
		tree.put( "nameé😀\"\\\n", "a<>&='é中😀\b\f\n\r\t\u0000\u2028\u2029\ud800x\udfff" );
		ArrayNode values = new ArrayNode( JsonNodeFactory.instance );
		values.add( (JsonNode) null );
		values.add( true );
		values.add( new JsonParsing().parse( "-0.12300e+004" ) );
		values.add( new ArrayNode( JsonNodeFactory.instance ) );
		values.add( new ObjectNode( JsonNodeFactory.instance ) );
		tree.set( "values", values );
		return tree;
	}

	static byte[] synchronous(JsonBody body) throws IOException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		body.writeTo( output );
		byte[] expected = output.toByteArray();
		for ( JsonWriteStrategy strategy : JsonWriteStrategy.values() ) {
			output.reset();
			org.hibernate.search.util.json.internal.spi.JsonWriting.writeTo( body, output, strategy );
			assertThat( output.toByteArray() ).isEqualTo( expected );
		}
		return expected;
	}

	static byte[] jackson(JsonNode tree) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		JacksonJsonBodyWriter.write( tree, output );
		return output.toByteArray();
	}

	static byte[] drain(JsonBody body, int capacity) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try ( JsonWriteCursor cursor = body.openCursor() ) {
			ByteBuffer buffer = ByteBuffer.allocate( capacity );
			while ( !cursor.isComplete() ) {
				cursor.write( buffer );
				output.write( buffer.array(), 0, buffer.position() );
				buffer.clear();
			}
		}
		return output.toByteArray();
	}
}
