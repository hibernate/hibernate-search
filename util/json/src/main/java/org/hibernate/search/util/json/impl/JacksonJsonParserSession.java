package org.hibernate.search.util.json.impl;

import java.nio.ByteBuffer;
import java.util.Objects;

import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.logging.impl.JsonLog;
import org.hibernate.search.util.json.spi.JsonParseException;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.async.ByteBufferFeeder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.BooleanNode;
import tools.jackson.databind.node.NullNode;
import tools.jackson.databind.node.StringNode;

final class JacksonJsonParserSession implements JsonParserSession {
	private static final ByteBuffer EMPTY = ByteBuffer.allocate( 0 ).asReadOnlyBuffer();
	private JsonParser parser;
	private ByteBufferFeeder feeder;
	private final JsonTreeBuilder builder = new JsonTreeBuilder();
	private final Utf8Validator utf8 = new Utf8Validator();
	private final JsonParsingOptions options;
	private long bytes;

	JacksonJsonParserSession(JsonParser parser, JsonParsingOptions options) {
		this.parser = parser;
		feeder = (ByteBufferFeeder) parser.nonBlockingInputFeeder();
		this.options = options;
	}

	@Override
	public void feed(ByteBuffer input) {
		requireOpen();
		Objects.requireNonNull( input );
		if ( !input.hasRemaining() ) {
			return;
		}
		int size = Math.min( input.remaining(), options.maxInputChunkSize() );
		if ( options.maxInputBytes() != 0 && size > options.maxInputBytes() - bytes ) {
			fail( JsonLog.INSTANCE.inputByteLimitExceeded( options.maxInputBytes() ) );
		}
		ByteBuffer slice = input.slice();
		slice.limit( size );
		try {
			if ( !feeder.needMoreInput() ) {
				throw JsonLog.INSTANCE.parserOwnsPreviousInput();
			}
			utf8.accept( slice );
			feeder.feedInput( slice );
			bytes += size;
			drain( false );
			// Jackson uses absolute reads without advancing the buffer position.
			input.position( input.position() + size );
			// Drop Jackson's reference to caller storage before returning ownership.
			feeder.feedInput( EMPTY );
		}
		catch (RuntimeException e) {
			fail( e );
		}
	}

	@Override
	public JsonNode endOfInput() {
		requireOpen();
		try {
			utf8.endOfInput();
			feeder.endOfInput();
			drain( true );
			JsonNode result = builder.finish();
			close();
			return result;
		}
		catch (RuntimeException e) {
			fail( e );
			return null;
		}
	}

	private void drain(boolean ended) {
		while ( true ) {
			JsonToken token = parser.nextToken();
			if ( token == JsonToken.NOT_AVAILABLE ) {
				if ( ended ) {
					throw JsonLog.INSTANCE.truncatedInput();
				}
				return;
			}
			if ( token == null ) {
				return;
			}
			switch ( token ) {
				case START_OBJECT:
					builder.startObject();
					break;
				case START_ARRAY:
					builder.startArray();
					break;
				case END_OBJECT:
				case END_ARRAY:
					builder.endContainer();
					break;
				case PROPERTY_NAME:
					builder.name( parser.currentName() );
					break;
				case VALUE_STRING:
					builder.add( StringNode.valueOf( parser.getString() ) );
					break;
				case VALUE_NUMBER_INT:
					builder.add( JsonNumbers.fromIntegerToken( parser.getString() ) );
					break;
				case VALUE_NUMBER_FLOAT:
					builder.add( JsonNumbers.fromFloatingToken( parser.getString() ) );
					break;
				case VALUE_TRUE:
					builder.add( BooleanNode.TRUE );
					break;
				case VALUE_FALSE:
					builder.add( BooleanNode.FALSE );
					break;
				case VALUE_NULL:
					builder.add( NullNode.instance );
					break;
				default:
					throw JsonLog.INSTANCE.unexpectedToken( String.valueOf( token ) );
			}
		}
	}

	private void fail(Exception failure) {
		close();
		if ( failure instanceof JsonParseException ) {
			throw (JsonParseException) failure;
		}
		throw JsonLog.INSTANCE.invalidInput( failure.getMessage(), failure );
	}

	private void requireOpen() {
		if ( parser == null ) {
			throw JsonLog.INSTANCE.parserClosed();
		}
	}

	@Override
	public long inputBytes() {
		return bytes;
	}

	@Override
	public void close() {
		JsonParser current = parser;
		parser = null;
		feeder = null;
		builder.clear();
		if ( current != null ) {
			try {
				current.close();
			}
			catch (RuntimeException e) {
				throw JsonLog.INSTANCE.cannotCloseParser( e );
			}
		}
	}
}
