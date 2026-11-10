package org.hibernate.search.util.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.util.json.internal.spi.JsonBodies;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.IntNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class JsonRawContainerOutputTest {
	@Test
	void rawMapNullNameOrValueCannotProduceMalformedJson() {
		Map<String, JsonNode> map = new HashMap<>();
		map.put( null, IntNode.valueOf( 1 ) );
		assertRejectedAndClosed( new ObjectNode( JsonNodeFactory.instance, map ) );
		map.clear();
		map.put( "value", null );
		assertRejectedAndClosed( new ObjectNode( JsonNodeFactory.instance, map ) );
	}

	@Test
	void rawListNullCannotSilentlyDropAnArrayElement() {
		List<JsonNode> values = new ArrayList<>();
		values.add( null );
		assertRejectedAndClosed( new ArrayNode( JsonNodeFactory.instance, values ) );
	}

	@Test
	void invalidLateNdjsonPartFailsItsTraversalAndReplayIndependently() {
		Map<String, JsonNode> map = new HashMap<>();
		map.put( "value", null );
		var body = JsonBodies.withContentLengthProbe( new NdjsonBody( List.of(
				new ObjectNode( JsonNodeFactory.instance ).put( "valid", "x".repeat( 9000 ) ),
				new ObjectNode( JsonNodeFactory.instance, map ) ) ), 8192 );
		assertThat( body.contentLength() ).isEqualTo( -1 );
		for ( int replay = 0; replay < 2; replay++ ) {
			try ( var cursor = body.openCursor() ) {
				assertThatThrownBy( () -> {
					while ( !cursor.isComplete() ) {
						cursor.write( ByteBuffer.allocate( 17 ) );
					}
				} ).isInstanceOf( IllegalArgumentException.class );
				assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ) ) )
						.isInstanceOf( IllegalStateException.class ).hasMessageContaining( "closed" );
			}
		}
	}

	private static void assertRejectedAndClosed(JsonNode value) {
		assertThatThrownBy( () -> JsonBodies.of( value ) ).isInstanceOf( IllegalArgumentException.class );
		try ( var cursor = new Utf8JsonWriteCursor( value ) ) {
			assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 64 ) ) )
					.isInstanceOf( IllegalArgumentException.class );
			assertThatThrownBy( () -> cursor.write( ByteBuffer.allocate( 1 ) ) )
					.isInstanceOf( IllegalStateException.class ).hasMessageContaining( "closed" );
		}
	}
}
