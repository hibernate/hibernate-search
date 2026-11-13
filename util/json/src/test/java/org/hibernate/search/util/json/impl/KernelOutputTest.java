package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.List;

import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.POJONode;
import tools.jackson.databind.node.StringNode;

class KernelOutputTest {

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void unicodeControlsLargeScalarAndNumbersPauseWithSixEncodedBytes(int capacity) throws Exception {
		var tree = new ObjectNode( JsonNodeFactory.instance );
		tree.put( "text", "é😀\n<".repeat( 16384 ) );
		tree.set( "number", BigIntegerNode.valueOf( new BigInteger( "1" + "0".repeat( 4095 ) ) ) );
		byte[] expected = ( "{\"text\":\"" + "é😀\\n\\u003c".repeat( 16384 ) + "\",\"number\":1" + "0".repeat( 4095 ) + "}" )
				.getBytes( StandardCharsets.UTF_8 );
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try ( var cursor = new Utf8JsonWriteCursor( tree ) ) {
			assertThat( cursor.write( ByteBuffer.allocate( 0 ) ) ).isZero();
			assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ).asReadOnlyBuffer() ) )
					.isInstanceOf( ReadOnlyBufferException.class );
			ByteBuffer buffer = ByteBuffer.allocate( capacity );
			int maxNumericChars = 0;
			while ( !cursor.isComplete() ) {
				int limit = buffer.limit();
				int count = cursor.write( buffer );
				assertThat( count ).isEqualTo( buffer.position() );
				assertThat( buffer.limit() ).isEqualTo( limit );
				assertThat( cursor.retainedEncodedCapacity() ).isLessThanOrEqualTo( 6 );
				maxNumericChars = Math.max( maxNumericChars, cursor.retainedGeneratedNumericCharacters() );
				output.write( buffer.array(), 0, buffer.position() );
				buffer.clear();
			}
			assertReleased( cursor );
			if ( capacity < 4096 ) {
				assertThat( maxNumericChars ).isEqualTo( 4096 );
			}
		}
		assertThat( output.toByteArray() ).isEqualTo( expected );
	}

	@Test
	void probeIsBoundedStableAndClosesItsTraversal() throws Exception {
		var root = StringNode.valueOf( "x".repeat( 1024 * 1024 ) );
		int[] writtenAndCloses = new int[2];
		JsonBody instrumented = () -> new JsonWriteCursor() {

			final Utf8JsonWriteCursor delegate = new Utf8JsonWriteCursor( root );

			public int write(ByteBuffer destination) {
				int count = delegate.write( destination );
				writtenAndCloses[0] += count;
				return count;
			}

			public boolean isComplete() {
				return delegate.isComplete();
			}

			public void close() {
				delegate.close();
				writtenAndCloses[1]++;
				assertReleased( delegate );
			}
		};
		JsonBody probed = JsonBodies.withContentLengthProbe( instrumented, 8192 );
		assertThat( writtenAndCloses ).containsExactly( 8192, 1 );
		assertThat( probed.contentLength() ).isEqualTo( -1 );
		assertThat( probed.contentLength() ).isEqualTo( -1 );
		var body = JsonBodies.ndjson( List.of( root ) );
		assertThat( body.contentLength() ).isEqualTo( -1 );
		byte[] first = bytes( body );
		byte[] second = bytes( body );
		assertThat( first ).isEqualTo( second );
		assertThat( first.length ).isEqualTo( 1024 * 1024 + 3 );
		assertThat( body.contentLength() ).isEqualTo( -1 );
		assertThat( MessageDigest.getInstance( "SHA-256" ).digest( streamBytes( body ) ) )
				.isEqualTo( MessageDigest.getInstance( "SHA-256" ).digest( first ) );
	}

	@Test
	void ndjsonFramesEmptyAndSmallBodiesAndRejectsNullPartsUpFront() throws Exception {
		var parsing = new JsonParsing();
		var empty = JsonBodies.ndjson( List.of() );
		assertThat( empty.contentLength() ).isZero();
		assertThat( bytes( empty ) ).isEmpty();
		var body = JsonBodies.ndjson( List.of( parsing.parse( "{}" ), parsing.parse( "[]" ) ) );
		assertThat( body.contentLength() ).isEqualTo( 6 );
		assertThat( new String( bytes( body ), StandardCharsets.UTF_8 ) ).isEqualTo( "{}\n[]\n" );
		assertThatThrownBy( () -> JsonBodies.ndjson( Arrays.asList( StringNode.valueOf( "x".repeat( 10000 ) ), null ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "NDJSON part" );
	}

	@Test
	void cancellationAndFailedSynchronousSinkReleaseState() throws Exception {
		var cursor = new Utf8JsonWriteCursor( StringNode.valueOf( "x".repeat( 1000000 ) ) );
		JsonBody body = () -> cursor;
		assertThatThrownBy( () -> body.writeTo( new OutputStream() {

			public void write(int value) throws IOException {
				throw new IOException( "failure" );
			}
		} ) ).isInstanceOf( IOException.class );
		assertReleased( cursor );
		assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ) ) ).isInstanceOf( IllegalStateException.class );
		var signingCursor = new Utf8JsonWriteCursor( StringNode.valueOf( "x".repeat( 1000000 ) ) );
		JsonBody signing = () -> signingCursor;
		try ( var stream = signing.openStream() ) {
			stream.read();
		}
		assertReleased( signingCursor );
	}

	@Test
	void deepTraversalIsIterativeAndCyclesNonFiniteAndPojoFail() {
		var root = new ArrayNode( JsonNodeFactory.instance );
		var node = root;
		for ( int depth = 1; depth < 20000; depth++ ) {
			var child = new ArrayNode( JsonNodeFactory.instance );
			node.add( child );
			node = child;
		}
		node.add( "😀" );
		try ( var cursor = new Utf8JsonWriteCursor( root ) ) {
			cursor.write( ByteBuffer.allocate( 20001 ) );
			assertThat( cursor.retainedTraversalDepth() ).isEqualTo( 20000 );
			cursor.cancel();
			assertReleased( cursor );
		}
		var cycle = new ArrayNode( JsonNodeFactory.instance );
		cycle.add( cycle );
		assertThatThrownBy( () -> JsonBodies.of( cycle ) ).isInstanceOf( IllegalArgumentException.class )
				.hasMessageContaining( "Cyclic" );
		assertThatThrownBy( () -> JsonBodies.of( DoubleNode.valueOf( Double.NaN ) ) )
				.isInstanceOf( IllegalArgumentException.class );
		assertThatThrownBy( () -> JsonBodies.of( new POJONode( new Object() ) ) )
				.isInstanceOf( IllegalArgumentException.class );
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void ndjsonTinyBuffersHaveIndependentReplayAndCallerRegionOwnership(int capacity) throws IOException {
		var parsing = new JsonParsing();
		var body = JsonBodies.ndjson( List.of( parsing.parse( "{\"text\":\"é😀\"}" ), parsing.parse( "{}" ) ) );
		byte[] expected = "{\"text\":\"é😀\"}\n{}\n".getBytes( StandardCharsets.UTF_8 );
		for ( int replay = 0; replay < 2; replay++ ) {
			var output = new ByteArrayOutputStream();
			try ( var cursor = body.openCursor() ) {
				ByteBuffer destination = ByteBuffer.allocate( capacity + 4 );
				while ( !cursor.isComplete() ) {
					destination.clear();
					destination.position( 2 );
					destination.limit( 2 + capacity );
					int count = cursor.write( destination );
					assertThat( destination.position() ).isEqualTo( 2 + count );
					assertThat( destination.limit() ).isEqualTo( 2 + capacity );
					output.write( destination.array(), 2, count );
				}
			}
			assertThat( output.toByteArray() ).isEqualTo( expected );
		}
	}

	private static byte[] streamBytes(JsonBody body) throws IOException {
		try ( var stream = body.openStream() ) {
			return stream.readAllBytes();
		}
	}

	private static byte[] bytes(JsonBody body) throws IOException {
		var output = new ByteArrayOutputStream();
		body.writeTo( output );
		return output.toByteArray();
	}

	private static void assertReleased(Utf8JsonWriteCursor cursor) {
		assertThat( cursor.retainedEncodedCapacity() ).isZero();
		assertThat( cursor.retainedTraversalDepth() ).isZero();
		assertThat( cursor.retainsScalarOrNextValue() ).isFalse();
		assertThat( cursor.retainedGeneratedNumericCharacters() ).isZero();
	}
}
