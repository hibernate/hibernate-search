package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.json.JsonTestNodes.scalar;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.BigIntegerNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class JsonOutputRetentionTest {
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void largeSingleNamesStringsNumbersAndBulksKeepSixEncodedBytes(int capacity) throws NoSuchAlgorithmException {
		for ( int size : new int[] { 16, 1024 * 1024 } ) {
			ObjectNode tree = new ObjectNode( JsonNodeFactory.instance );
			tree.put( "n".repeat( size ), "é😀\n<".repeat( size / 8 ) );
			tree.set( "number", BigIntegerNode
					.valueOf( new BigInteger( "1" + "0".repeat( Math.min( size, 4096 ) - 1 ) ) ) );
			ArrayNode smallBulk = new ArrayNode( JsonNodeFactory.instance );
			for ( int i = 0; i < size / 128; i++ ) {
				smallBulk.add( scalar( "a".repeat( 128 ) ) );
			}
			for ( JsonNode root : new JsonNode[] { tree, smallBulk } ) {
				MessageDigest expected = MessageDigest.getInstance( "SHA-256" );
				JacksonJsonBodyWriter.write( root, new DigestOutputStream( OutputStream.nullOutputStream(), expected ) );
				MessageDigest actual = MessageDigest.getInstance( "SHA-256" );
				ByteBuffer buffer = ByteBuffer.allocate( capacity );
				try ( Utf8JsonWriteCursor cursor = new Utf8JsonWriteCursor( root ) ) {
					assertThat( cursor.write( ByteBuffer.allocate( 0 ) ) ).isZero();
					assertThat( cursor.retainedTraversalDepth() ).isZero();
					int maximumEncoded = 0;
					int maximumDepth = 0;
					long count = 0;
					while ( !cursor.isComplete() ) {
						count += cursor.write( buffer );
						actual.update( buffer.array(), 0, buffer.position() );
						maximumEncoded = Math.max( maximumEncoded, cursor.retainedEncodedCapacity() );
						maximumDepth = Math.max( maximumDepth, cursor.retainedTraversalDepth() );
						buffer.clear();
					}
					assertThat( count ).isPositive();
					assertThat( actual.digest() ).isEqualTo( expected.digest() );
					assertThat( maximumEncoded ).isLessThanOrEqualTo( 6 );
					assertThat( maximumDepth ).isLessThanOrEqualTo( 1 );
					assertReleased( cursor );
				}
			}
		}
	}

	@Test
	void veryDeepTraversalIsIterativeAndCancellationReleasesFramesAndScalar() {
		ArrayNode root = new ArrayNode( JsonNodeFactory.instance );
		ArrayNode node = root;
		int depth = 50_000;
		for ( int i = 1; i < depth; i++ ) {
			ArrayNode child = new ArrayNode( JsonNodeFactory.instance );
			node.add( child );
			node = child;
		}
		node.add( "😀".repeat( 8192 ) );
		try ( Utf8JsonWriteCursor cursor = new Utf8JsonWriteCursor( root ) ) {
			ByteBuffer buffer = ByteBuffer.allocate( depth + 2 );
			assertThat( cursor.write( buffer ) ).isEqualTo( depth + 2 );
			assertThat( cursor.retainedTraversalDepth() ).isEqualTo( depth );
			assertThat( cursor.retainsScalarOrNextValue() ).isTrue();
			assertThat( cursor.retainedEncodedCapacity() ).isEqualTo( 6 );
			cursor.cancel();
			assertReleased( cursor );
			assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ) ) )
					.isInstanceOf( IllegalStateException.class );
		}
		try ( Utf8JsonWriteCursor cursor = new Utf8JsonWriteCursor( root ) ) {
			long count = 0;
			ByteBuffer buffer = ByteBuffer.allocate( 5 );
			while ( !cursor.isComplete() ) {
				count += cursor.write( buffer );
				buffer.clear();
			}
			assertThat( count ).isEqualTo( depth * 2L + 2 + 8192 * 4 );
			assertReleased( cursor );
		}
	}

	@Test
	void probeStopsWithinHugeNumberAndAlwaysClosesItsCursor() {
		JsonNode number =
				BigIntegerNode.valueOf( new BigInteger( "1" + "0".repeat( 16383 ) ) );
		AtomicInteger written = new AtomicInteger();
		AtomicInteger closes = new AtomicInteger();
		JsonBody source = () -> new JsonWriteCursor() {
			private final Utf8JsonWriteCursor delegate = new Utf8JsonWriteCursor( number );

			@Override
			public int write(ByteBuffer destination) {
				int count = delegate.write( destination );
				written.addAndGet( count );
				return count;
			}

			@Override
			public boolean isComplete() {
				return delegate.isComplete();
			}

			@Override
			public void close() {
				delegate.close();
				closes.incrementAndGet();
				assertReleased( delegate );
			}
		};
		JsonBody body = JsonBodies.withContentLengthProbe( source, 8192 );
		assertThat( written ).hasValue( 8192 );
		assertThat( closes ).hasValue( 1 );
		assertThat( body.contentLength() ).isEqualTo( -1 );
		assertThat( body.contentLength() ).isEqualTo( -1 );
		assertThat( written ).hasValue( 8192 );
	}

	@Test
	void synchronousFailureAndSigningStreamCloseReleaseCursor() {
		Utf8JsonWriteCursor write = new Utf8JsonWriteCursor( scalar( "x".repeat( 1_000_000 ) ) );
		JsonBody source = () -> write;
		assertThatThrownBy( () -> source.writeTo( new OutputStream() {
			@Override
			public void write(int value) throws IOException {
				throw new IOException( "failure" );
			}
		} ) ).isInstanceOf( IOException.class );
		assertReleased( write );
		Utf8JsonWriteCursor read = new Utf8JsonWriteCursor( scalar( "x".repeat( 1_000_000 ) ) );
		JsonBody signing = () -> read;
		assertThat( signing.openStream() ).satisfies( stream -> {
			try {
				stream.read();
				stream.close();
			}
			catch (IOException e) {
				throw new IllegalStateException( e );
			}
		} );
		assertReleased( read );
	}

	private static void assertReleased(Utf8JsonWriteCursor cursor) {
		assertThat( cursor.retainedEncodedCapacity() ).isZero();
		assertThat( cursor.retainedTraversalDepth() ).isZero();
		assertThat( cursor.retainsScalarOrNextValue() ).isFalse();
	}
}
