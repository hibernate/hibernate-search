package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonTreeValidation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.DoubleNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.LongNode;
import tools.jackson.databind.node.ShortNode;
import tools.jackson.databind.node.StringNode;

class KernelOutputV2Test {

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void primitiveExtremesAndAlternatingScalarsUseBoundedCharactersWithoutDerivedStrings(int capacity) {
		String text =
				"[-32768,32767,-2147483648,2147483647,-9223372036854775808,9223372036854775807,0,-1,42,9007199254740993,\"after\",true,null]";
		JsonNode tree = new JsonParsing().parse( text );
		for ( String kind : new String[] { "heap", "direct", "slice" } ) {
			var out = new ByteArrayOutputStream();
			try ( var cursor = new Utf8JsonWriteCursor( tree ) ) {
				ByteBuffer storage = "direct".equals( kind )
						? ByteBuffer.allocateDirect( capacity + 8 )
						: ByteBuffer.allocate( capacity + 8 );
				ByteBuffer buffer = "slice".equals( kind ) ? storage.position( 3 ).slice() : storage;
				int maximumCharacters = 0;
				while ( !cursor.isComplete() ) {
					buffer.clear().position( 2 ).limit( capacity + 2 );
					int count = cursor.write( buffer );
					assertThat( buffer.position() ).isEqualTo( count + 2 );
					assertThat( buffer.limit() ).isEqualTo( capacity + 2 );
					if ( "slice".equals( kind ) ) {
						for ( int prefix = 0; prefix < 5; prefix++ ) {
							assertThat( storage.get( prefix ) ).isZero();
						}
					}
					assertThat( cursor.retainedGeneratedNumericCharacters() ).isZero();
					assertThat( cursor.retainedEncodedCapacity() ).isLessThanOrEqualTo( 6 );
					maximumCharacters = Math.max( maximumCharacters, cursor.retainedIntegralCharacterCapacity() );
					for ( int index = 2; index < buffer.position(); index++ ) {
						out.write( buffer.get( index ) );
					}
				}
				if ( capacity < 20 ) {
					assertThat( maximumCharacters ).isEqualTo( 20 );
				}
				assertThat( cursor.retainedIntegralCharacterCapacity() ).isZero();
				assertThat( cursor.retainedCycleEntries() ).isZero();
			}
			assertThat( out.toString( StandardCharsets.UTF_8 ) ).isEqualTo( text );
		}
		var shortTree = new ArrayNode( JsonNodeFactory.instance ).add( ShortNode.valueOf( (short) -123 ) );
		assertThat( render( shortTree ) ).isEqualTo( "[-123]" );
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 15, 16, 17, 40 })
	void cyclesAreDetectedOnBothSidesOfHashThresholdAndReleaseReferences(int depth) {
		var root = chain( depth );
		ArrayNode tail = root;
		for ( int index = 1; index < depth; index++ ) {
			tail = (ArrayNode) tail.get( 0 );
		}
		tail.add( root );
		assertThatThrownBy( () -> JsonBodies.openCursor( root ) ).isInstanceOf( IllegalArgumentException.class );
		if ( !JsonTreeValidation.isInternalValidationEnabled() ) {
			// Deliberately invalid internal trees are only traversed with debug cycle tracking enabled.
			return;
		}
		var cursor = new Utf8JsonWriteCursor( root );
		assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( depth + 10 ) ) )
				.isInstanceOf( IllegalArgumentException.class ).hasMessageContaining( "Cyclic" );
		assertThat( cursor.retainedCycleEntries() ).isZero();
		assertThat( cursor.retainedTraversalDepth() ).isZero();
		assertThat( cursor.retainedEncodedCapacity() ).isZero();
		assertThat( cursor.retainsScalarOrNextValue() ).isFalse();
		assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ) ) ).isInstanceOf( IllegalStateException.class );
	}

	@Test
	void shallowCycleChecksAllocateNoMapAndDeepCancellationClearsActiveKeys() throws Exception {
		try ( var shallow = new Utf8JsonWriteCursor( chain( 15 ) ) ) {
			shallow.write( ByteBuffer.allocate( 15 ) );
			assertThat( field( shallow, "workspace" ) ).isNull();
			assertThat( shallow.retainedTraversalDepth() ).isEqualTo( 15 );
		}
		var cursor = new Utf8JsonWriteCursor( chain( 40 ) );
		cursor.write( ByteBuffer.allocate( 40 ) );
		JsonWriteWorkspace workspace = (JsonWriteWorkspace) field( cursor, "workspace" );
		if ( JsonTreeValidation.isInternalValidationEnabled() ) {
			assertThat( workspace.active ).hasSize( 40 );
		}
		else {
			assertThat( workspace ).isNull();
		}
		cursor.cancel();
		if ( workspace != null ) {
			assertThat( workspace.active ).isEmpty();
		}
		assertThat( field( cursor, "workspace" ) ).isNull();
	}

	@Test
	void sequentialNdjsonPartsReuseWorkspaceButConcurrentCursorsRemainIndependent() throws Exception {
		List<JsonNode> parts = new ArrayList<>();
		parts.add( new LongNode( Long.MIN_VALUE ) );
		parts.add( new LongNode( Long.MAX_VALUE ) );
		// Include two uses of one deep acyclic node to exercise hashing reuse and DAG acceptance.
		var shared = chain( 40 );
		parts.add( shared );
		parts.add( shared );
		var body = JsonBodies.ndjson( parts );
		var out = new ByteArrayOutputStream();
		try ( var first = body.openCursor();
				var replay = body.openCursor() ) {
			JsonWriteWorkspace state = (JsonWriteWorkspace) field( first, "workspace" );
			JsonWriteWorkspace other = (JsonWriteWorkspace) field( replay, "workspace" );
			assertThat( state ).isNotSameAs( other );
			char[] firstIntegral = null;
			int newlines = 0;
			while ( !first.isComplete() ) {
				ByteBuffer one = ByteBuffer.allocate( 1 );
				first.write( one );
				if ( !JsonTreeValidation.isInternalValidationEnabled() ) {
					assertThat( state.active ).isNull();
				}
				if ( one.position() == 0 ) {
					continue;
				}
				byte emitted = one.get( 0 );
				out.write( emitted );
				if ( state.integral != null && firstIntegral == null ) {
					firstIntegral = state.integral;
				}
				if ( emitted == '\n' ) {
					newlines++;
					assertThat( state.integral ).hasSize( 20 ).isSameAs( firstIntegral );
					if ( newlines < parts.size() ) {
						assertThat( field( first, "workspace" ) ).isSameAs( state );
					}
					if ( state.active != null ) {
						assertThat( state.active ).isEmpty();
					}
				}
			}
			assertThat( newlines ).isEqualTo( parts.size() );
			assertThat( field( first, "workspace" ) ).isNull();
			assertThat( other.integral ).isNull();
			var replayed = new ByteArrayOutputStream();
			while ( !replay.isComplete() ) {
				ByteBuffer b = ByteBuffer.allocate( 5 );
				replay.write( b );
				replayed.write( b.array(), 0, b.position() );
			}
			assertThat( replayed.toByteArray() ).isEqualTo( out.toByteArray() );
			assertThat( other.integral ).isNotSameAs( state.integral );
		}
		String lines = out.toString( StandardCharsets.UTF_8 );
		assertThat( lines ).startsWith( "-9223372036854775808\n9223372036854775807\n" );
	}

	@Test
	void failedNdjsonWriteClearsOuterAndSharedStateAndReadonlyPreflightPreservesCursor() throws Exception {
		var body = JsonBodies.withContentLengthProbe( new NdjsonBody(
				List.of( StringNode.valueOf( "x".repeat( 10000 ) ), DoubleNode.valueOf( Double.NaN ) ) ), 8192 );
		var cursor = body.openCursor();
		JsonWriteWorkspace state = (JsonWriteWorkspace) field( cursor, "workspace" );
		assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ).asReadOnlyBuffer() ) )
				.isInstanceOf( ReadOnlyBufferException.class );
		assertThat( field( cursor, "workspace" ) ).isSameAs( state );
		assertThatThrownBy( () -> {
			while ( !cursor.isComplete() ) {
				cursor.write( ByteBuffer.allocate( 8192 ) );
			}
		} ).isInstanceOf( IllegalArgumentException.class );
		assertThat( field( cursor, "parts" ) ).isNull();
		assertThat( field( cursor, "part" ) ).isNull();
		assertThat( field( cursor, "workspace" ) ).isNull();
		if ( state.active != null ) {
			assertThat( state.active ).isEmpty();
		}
	}

	private static ArrayNode chain(int depth) {
		var root = new ArrayNode( JsonNodeFactory.instance );
		var current = root;
		for ( int index = 1; index < depth; index++ ) {
			var child = new ArrayNode( JsonNodeFactory.instance );
			current.add( child );
			current = child;
		}
		return root;
	}

	private static Object field(Object object, String name) throws Exception {
		var field = object.getClass().getDeclaredField( name );
		field.setAccessible( true );
		return field.get( object );
	}

	private static String render(JsonNode value) {
		var out = new ByteArrayOutputStream();
		try {
			JsonBodies.of( value ).writeTo( out );
		}
		catch (IOException failure) {
			throw new UncheckedIOException( failure );
		}
		return out.toString( StandardCharsets.UTF_8 );
	}
}
