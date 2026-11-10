package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.hibernate.search.util.json.internal.spi.JsonParsing;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.LongNode;

class KernelOutputV3Test {

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3, 10, 11, 19, 20, 21, 8192 })
	void primitiveRenderingNeverWritesOutsideCallerSlice(int capacity) {
		long[] numbers = {
				0,
				1,
				9,
				10,
				99,
				100,
				-1,
				-9,
				-10,
				-99,
				-100,
				Integer.MIN_VALUE,
				Integer.MAX_VALUE,
				(long) Integer.MIN_VALUE - 1,
				(long) Integer.MAX_VALUE + 1,
				Long.MIN_VALUE,
				Long.MAX_VALUE };
		var root = new ArrayNode( JsonNodeFactory.instance );
		var expected = new StringBuilder( "[" );
		for ( int index = 0; index < numbers.length; index++ ) {
			// Alternate Int and Long nodes, including Long nodes containing a small integer.
			long number = numbers[index];
			if ( index % 2 == 0 && number >= Integer.MIN_VALUE && number <= Integer.MAX_VALUE ) {
				root.add( new IntNode( (int) number ) );
			}
			else {
				root.add( new LongNode( number ) );
			}
			if ( index > 0 ) {
				expected.append( ',' );
			}
			expected.append( number );
		}
		expected.append( ']' );
		var output = new ByteArrayOutputStream();
		try ( var cursor = new Utf8JsonWriteCursor( root ) ) {
			byte[] storage = new byte[capacity + 12];
			var buffer = ByteBuffer.wrap( storage ).position( 3 ).slice();
			while ( !cursor.isComplete() ) {
				Arrays.fill( storage, (byte) 0x55 );
				buffer.clear().position( 2 ).limit( capacity + 2 );
				int count = cursor.write( buffer );
				assertThat( count ).isEqualTo( buffer.position() - 2 ).isPositive();
				for ( int index = 0; index < 5; index++ ) {
					assertThat( storage[index] ).isEqualTo( (byte) 0x55 );
				}
				for ( int index = 5 + count; index < storage.length; index++ ) {
					assertThat( storage[index] ).isEqualTo( (byte) 0x55 );
				}
				output.write( storage, 5, count );
				assertThat( cursor.retainedGeneratedNumericCharacters() ).isZero();
				assertThat( cursor.retainedIntegralCharacterCapacity() ).isLessThanOrEqualTo( 20 );
				assertThat( cursor.retainedEncodedCapacity() ).isLessThanOrEqualTo( 6 );
			}
		}
		assertThat( output.toString( StandardCharsets.UTF_8 ) ).isEqualTo( expected.toString() );
	}

	@Test
	void completeHeapPrimitivesUseCallerStorageWithoutAllocatingCharacterFallback() throws Exception {
		var root = new JsonParsing().parse( "[10,-99,2147483647,-2147483648,9223372036854775807,-9223372036854775808]" );
		try ( var cursor = new Utf8JsonWriteCursor( root ) ) {
			var buffer = ByteBuffer.allocate( 8192 );
			cursor.write( buffer );
			assertThat( cursor.isComplete() ).isTrue();
			assertThat( field( cursor, "workspace" ) ).isNull();
		}
		// Keep a later large string active so the numeric fallback's allocation is observable.
		var value = new ArrayNode( JsonNodeFactory.instance ).add( Long.MIN_VALUE ).add( "x".repeat( 100 ) );
		try ( var cursor = new Utf8JsonWriteCursor( value ) ) {
			cursor.write( ByteBuffer.allocate( 21 ) );
			assertThat( cursor.isComplete() ).isFalse();
			assertThat( field( cursor, "workspace" ) ).isNull();
			assertThat( cursor.retainedIntegralCharacterCapacity() ).isZero();
		}
		try ( var cursor = new Utf8JsonWriteCursor( value ) ) {
			cursor.write( ByteBuffer.allocateDirect( 21 ) );
			assertThat( cursor.isComplete() ).isFalse();
			assertThat( cursor.retainedIntegralCharacterCapacity() ).isEqualTo( 20 );
			cursor.cancel();
			assertThat( cursor.retainedIntegralCharacterCapacity() ).isZero();
			assertThat( cursor.retainedTraversalDepth() ).isZero();
		}
	}

	private static Object field(Object object, String name) throws Exception {
		var field = object.getClass().getDeclaredField( name );
		field.setAccessible( true );
		return field.get( object );
	}
}
