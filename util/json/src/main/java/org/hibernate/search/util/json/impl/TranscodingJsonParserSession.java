package org.hibernate.search.util.json.impl;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.logging.impl.JsonLog;
import org.hibernate.search.util.json.spi.JsonParseException;

import tools.jackson.databind.JsonNode;

/** Bounded incremental charset decoding and UTF-8 encoding. */
final class TranscodingJsonParserSession implements JsonParserSession {
	private static final int BYTE_BUFFER_SIZE = 8192;
	private static final int CHAR_BUFFER_SIZE = 4096;

	private JsonParserSession delegate;
	private CharsetDecoder decoder;
	private CharsetEncoder encoder;
	private ByteBuffer pending = ByteBuffer.allocate( BYTE_BUFFER_SIZE );
	private CharBuffer chars = CharBuffer.allocate( CHAR_BUFFER_SIZE );
	private ByteBuffer utf8 = ByteBuffer.allocate( BYTE_BUFFER_SIZE );
	private final JsonParsingOptions options;
	private long bytes;

	TranscodingJsonParserSession(JsonParserSession delegate, Charset charset, JsonParsingOptions options) {
		this.delegate = delegate;
		decoder = charset.newDecoder().onMalformedInput( CodingErrorAction.REPORT )
				.onUnmappableCharacter( CodingErrorAction.REPORT );
		encoder = StandardCharsets.UTF_8.newEncoder().onMalformedInput( CodingErrorAction.REPORT );
		this.options = options;
	}

	@Override
	public void feed(ByteBuffer input) {
		requireOpen();
		Objects.requireNonNull( input );
		if ( !input.hasRemaining() ) {
			return;
		}
		int size = Math.min( Math.min( input.remaining(), options.maxInputChunkSize() ), pending.remaining() );
		try {
			if ( size == 0 ) {
				throw JsonLog.INSTANCE.charsetDecoderCannotProgress();
			}
			if ( options.maxInputBytes() != 0 && size > options.maxInputBytes() - bytes ) {
				throw JsonLog.INSTANCE.inputByteLimitExceeded( options.maxInputBytes() );
			}
			ByteBuffer slice = input.slice();
			slice.limit( size );
			pending.put( slice );
			bytes += size;
			input.position( input.position() + size );
			pending.flip();
			decode( false );
			pending.compact();
		}
		catch (RuntimeException e) {
			fail( e );
		}
	}

	private void decode(boolean end) {
		while ( true ) {
			CoderResult result = decoder.decode( pending, chars, end );
			check( result );
			encode( false );
			if ( result.isUnderflow() ) {
				return;
			}
		}
	}

	private void encode(boolean end) {
		chars.flip();
		while ( true ) {
			CoderResult result = encoder.encode( chars, utf8, end );
			check( result );
			drainBytes();
			if ( result.isUnderflow() ) {
				break;
			}
		}
		chars.compact();
	}

	private void drainBytes() {
		utf8.flip();
		while ( utf8.hasRemaining() ) {
			delegate.feed( utf8 );
		}
		utf8.clear();
	}

	private static void check(CoderResult result) {
		if ( result.isError() ) {
			throw JsonLog.INSTANCE.invalidCharsetInput( result.toString() );
		}
	}

	@Override
	public JsonNode endOfInput() {
		requireOpen();
		try {
			pending.flip();
			decode( true );
			while ( true ) {
				CoderResult result = decoder.flush( chars );
				check( result );
				encode( false );
				if ( result.isUnderflow() ) {
					break;
				}
			}
			encode( true );
			while ( true ) {
				CoderResult result = encoder.flush( utf8 );
				check( result );
				drainBytes();
				if ( result.isUnderflow() ) {
					break;
				}
			}
			JsonNode result = delegate.endOfInput();
			close();
			return result;
		}
		catch (RuntimeException e) {
			fail( e );
			return null;
		}
	}

	private void fail(RuntimeException failure) {
		close();
		if ( failure instanceof JsonParseException ) {
			throw failure;
		}
		throw JsonLog.INSTANCE.cannotTranscodeInput( failure.getMessage(), failure );
	}

	private void requireOpen() {
		if ( delegate == null ) {
			throw JsonLog.INSTANCE.parserClosed();
		}
	}

	@Override
	public long inputBytes() {
		return bytes;
	}

	@Override
	public void close() {
		JsonParserSession current = delegate;
		delegate = null;
		decoder = null;
		encoder = null;
		pending = null;
		chars = null;
		utf8 = null;
		if ( current != null ) {
			current.close();
		}
	}
}
