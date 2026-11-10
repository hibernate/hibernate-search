package org.hibernate.search.util.json.example;

import java.nio.ByteBuffer;

import tools.jackson.databind.JsonNode;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonParseException;
import org.hibernate.search.util.json.internal.spi.JsonParserSession;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.internal.spi.JsonParsingOptions;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import io.vertx.core.Context;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;

/** Incremental UTF-8 input and replayable output with one write in flight. */
public final class JsonEchoServer {
	private static final int CHUNK_SIZE = 8192;
	private static final JsonParsing PARSING = new JsonParsing(
			JsonParsingOptions.DEFAULT.withMaxInputBytes( 1024 * 1024 ) );

	public static void main(String[] args) {
		int port = args.length == 0 ? 8080 : Integer.parseInt( args[0] );
		Vertx vertx = Vertx.vertx();
		vertx.createHttpServer().requestHandler( request -> new Exchange( request ).start() )
				.listen( port, "127.0.0.1" )
				.onSuccess( server -> System.out.println( "JSON echo server: http://127.0.0.1:" + server.actualPort() ) )
				.onFailure( failure -> {
					failure.printStackTrace();
					vertx.close();
				} );
	}

	/** All callbacks and state transitions stay on this request's Vert.x context. */
	private static final class Exchange {
		private final HttpServerRequest request;
		private final HttpServerResponse response;
		private final Context context = Vertx.currentContext();
		private final byte[] inputScratch = new byte[CHUNK_SIZE];
		private final ByteBuffer outputScratch = ByteBuffer.allocate( CHUNK_SIZE );
		private JsonParserSession parser = PARSING.createParser();
		private JsonWriteCursor cursor;
		private boolean terminal;

		private Exchange(HttpServerRequest request) {
			this.request = request;
			response = request.response();
		}

		private void start() {
			request.exceptionHandler( this::abort );
			response.exceptionHandler( this::abort );
			response.closeHandler( ignored -> abort( new IllegalStateException( "Peer disconnected" ) ) );
			request.handler( this::feed );
			request.endHandler( ignored -> inputComplete() );
		}

		private void feed(Buffer chunk) {
			if ( terminal ) {
				return;
			}
			try {
				// Copy bounded slices through the public Vert.x API, not a full body.
				for ( int offset = 0; offset < chunk.length(); ) {
					int count = Math.min( inputScratch.length, chunk.length() - offset );
					chunk.getBytes( offset, offset + count, inputScratch, 0 );
					ByteBuffer input = ByteBuffer.wrap( inputScratch, 0, count );
					while ( input.hasRemaining() ) {
						parser.feed( input );
					}
					offset += count;
				}
			}
			catch (JsonParseException failure) {
				invalidInput();
			}
			catch (RuntimeException failure) {
				abort( failure );
			}
		}

		private void inputComplete() {
			if ( terminal ) {
				return;
			}
			try {
				JsonNode root = parser.endOfInput();
				closeParser();
				// This tree remains unchanged until the response completes.
				JsonBody body = JsonBodies.of( root );
				cursor = body.openCursor();
				response.putHeader( "Content-Type", "application/json; charset=utf-8" );
				if ( body.contentLength() >= 0 ) {
					response.putHeader( "Content-Length", Long.toString( body.contentLength() ) );
				}
				else {
					response.setChunked( true );
				}
				pump();
			}
			catch (JsonParseException failure) {
				invalidInput();
			}
			catch (RuntimeException failure) {
				abort( failure );
			}
		}

		private void pump() {
			if ( terminal ) {
				return;
			}
			try {
				if ( cursor.isComplete() ) {
					cursor.close();
					cursor = null;
					response.end().onSuccess( ignored -> terminal = true ).onFailure( this::abort );
					return;
				}
				outputScratch.clear();
				int written = cursor.write( outputScratch );
				// appendBytes copies the filled region into storage owned by this write.
				Buffer output = Buffer.buffer( written ).appendBytes( outputScratch.array(), 0, written );
				response.write( output ).onSuccess( ignored -> {
					// One write in flight; yield between chunks even if a future completes inline.
					context.runOnContext( next -> pump() );
				} ).onFailure( this::abort );
			}
			catch (RuntimeException failure) {
				abort( failure );
			}
		}

		private void invalidInput() {
			terminal = true;
			closeParser();
			// Existing input handlers observe terminal and discard any remaining chunks.
			// Do not replace handlers here: EOF may already have been delivered.
			response.setStatusCode( 400 ).putHeader( "Content-Type", "text/plain; charset=utf-8" )
					.end( "Expected one valid UTF-8 JSON value of at most 1 MiB.\n" )
					.onFailure( failure -> response.reset() );
		}

		private void abort(Throwable failure) {
			if ( terminal ) {
				return;
			}
			terminal = true;
			closeParser();
			if ( cursor != null ) {
				cursor.close();
				cursor = null;
			}
			response.reset();
		}

		private void closeParser() {
			if ( parser != null ) {
				parser.close();
				parser = null;
			}
		}
	}
}
