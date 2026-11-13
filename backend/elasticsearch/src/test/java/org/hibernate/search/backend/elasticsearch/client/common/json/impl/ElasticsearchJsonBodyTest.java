package org.hibernate.search.backend.elasticsearch.client.common.json.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.node.StringNode;

class ElasticsearchJsonBodyTest {
	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void deterministicFinalNewlineWithZeroAndPartialSinkAcceptance(int capacity) throws IOException {
		ObjectNode metadata = JsonNodeFactory.instance.objectNode();
		metadata.set( "index", JsonNodeFactory.instance.objectNode() );
		ObjectNode document = JsonNodeFactory.instance.objectNode();
		document.put( "text", "é😀<\n" );
		JsonBody body = ElasticsearchJsonBody.ndjson( List.of( metadata, document ) );
		ByteArrayOutputStream synchronous = new ByteArrayOutputStream();
		body.writeTo( synchronous );
		byte[] expected = synchronous.toByteArray();
		assertThat( new String( expected, StandardCharsets.UTF_8 ) )
				.isEqualTo( "{\"index\":{}}\n{\"text\":\"é😀\\u003c\\n\"}\n" );
		assertThat( body.contentLength() ).isEqualTo( expected.length );
		ByteBuffer buffer = ByteBuffer.allocate( capacity );
		buffer.limit( 0 );
		ByteArrayOutputStream partial = new ByteArrayOutputStream();
		int[] acceptance = { 0, 1, 0, 2, 5 };
		int attempt = 0;
		try ( JsonWriteCursor cursor = body.openCursor() ) {
			assertThat( cursor.write( ByteBuffer.allocate( 0 ) ) ).isZero();
			while ( !cursor.isComplete() || buffer.hasRemaining() ) {
				if ( !buffer.hasRemaining() && !cursor.isComplete() ) {
					buffer.clear();
					cursor.write( buffer );
					buffer.flip();
				}
				int accepted = Math.min( acceptance[attempt++ % acceptance.length], buffer.remaining() );
				for ( int i = 0; i < accepted; i++ ) {
					partial.write( buffer.get() );
				}
			}
		}
		assertThat( partial.toByteArray() ).isEqualTo( expected );
		try ( var signing = body.openStream() ) {
			assertThat( signing.readAllBytes() ).isEqualTo( expected );
		}
		try ( JsonWriteCursor retry = body.openCursor() ) {
			retry.write( ByteBuffer.allocate( 2 ) );
			retry.cancel();
			assertThatThrownBy( () -> retry.write( ByteBuffer.allocate( 1 ) ) ).isInstanceOf( IllegalStateException.class );
		}
		assertThat( drain( body, capacity ) ).isEqualTo( expected );
		assertThat( body.contentLength() ).isEqualTo( expected.length );
	}

	@Test
	void emptyAndSinglePartFraming() {
		JsonBody empty = ElasticsearchJsonBody.ndjson( List.of() );
		assertThat( empty.contentLength() ).isZero();
		assertThat( drain( empty, 1 ) ).isEmpty();
		try ( JsonWriteCursor cursor = empty.openCursor() ) {
			assertThat( cursor.isComplete() ).isTrue();
			assertThat( cursor.write( ByteBuffer.allocate( 0 ) ) ).isZero();
		}
		assertThat( drain( ElasticsearchJsonBody.ndjson( List.of( JsonNodeFactory.instance.objectNode() ) ), 1 ) )
				.isEqualTo( "{}\n".getBytes( StandardCharsets.UTF_8 ) );
	}

	@Test
	void largeBulkLengthRemainsUnknownAndIndependentCursorsInterleave() {
		List<JsonNode> parts = new ArrayList<>();
		for ( int i = 0; i < 10_000; i++ ) {
			parts.add( StringNode.valueOf( "é😀" ) );
		}
		JsonBody body = ElasticsearchJsonBody.ndjson( parts );
		assertThat( body.contentLength() ).isEqualTo( -1 );
		try ( JsonWriteCursor first = body.openCursor(); JsonWriteCursor second = body.openCursor() ) {
			ByteBuffer one = ByteBuffer.allocate( 1 );
			ByteBuffer two = ByteBuffer.allocate( 2 );
			long firstCount = 0;
			long secondCount = 0;
			while ( !first.isComplete() || !second.isComplete() ) {
				firstCount += first.write( one );
				secondCount += second.write( two );
				one.clear();
				two.clear();
			}
			assertThat( firstCount ).isEqualTo( 90_000 );
			assertThat( secondCount ).isEqualTo( firstCount );
		}
		assertThat( body.contentLength() ).isEqualTo( -1 );
	}

	@Test
	void requestEscapingAndNormalizedNumericValueMatchGsonForWellFormedUnicode() throws IOException {
		String unicode = "<>&='é中😀\n\t\b\f\r\u0000\u001f\u2028\u2029\"\\";
		ObjectNode tree = JsonNodeFactory.instance.objectNode();
		tree.put( unicode, unicode );
		tree.set( "number", new JsonParsing().parse( "-0.12300e+004" ) );
		JsonObject gsonTree = new JsonObject();
		gsonTree.addProperty( unicode, unicode );
		gsonTree.add( "number", JsonParser.parseString( tree.get( "number" ).numberValue().toString() ) );
		ByteArrayOutputStream baseline = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter( baseline, StandardCharsets.UTF_8 );
		new Gson().toJson( gsonTree, writer );
		writer.append( '\n' );
		writer.flush();
		assertThat( drain( ElasticsearchJsonBody.ndjson( List.of( tree ) ), 1 ) ).isEqualTo( baseline.toByteArray() );
	}

	@Test
	void loneSurrogateIsLosslesslyEscapedWhereGsonWriterReplacesIt() throws IOException {
		StringNode tree = StringNode.valueOf( "\ud800" );
		ByteArrayOutputStream baseline = new ByteArrayOutputStream();
		OutputStreamWriter writer = new OutputStreamWriter( baseline, StandardCharsets.UTF_8 );
		new Gson().toJson( new JsonPrimitive( "\ud800" ), writer );
		writer.flush();
		assertThat( baseline.toString( StandardCharsets.UTF_8 ) ).isEqualTo( "\"?\"" );
		assertThat( new String( drain( JsonBodies.of( tree ), 1 ), StandardCharsets.UTF_8 ) )
				.isEqualTo( "\"\\ud800\"" );
	}

	private static byte[] drain(JsonBody body, int capacity) {
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
