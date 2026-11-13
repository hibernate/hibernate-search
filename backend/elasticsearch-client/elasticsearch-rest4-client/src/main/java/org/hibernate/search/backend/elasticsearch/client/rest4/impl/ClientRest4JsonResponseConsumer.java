package org.hibernate.search.backend.elasticsearch.client.rest4.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CancellationException;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonGzipDecoder;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonResponseConsumerScope;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonTransportSupport;
import org.hibernate.search.backend.elasticsearch.client.rest4.logging.impl.ElasticsearchClientLog;
import org.hibernate.search.util.json.spi.JsonResponseConsumer;
import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.AbstractAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

/** Bounded input storage; materialized JSON and bounded failure diagnostics replace the buffered entity. */
final class ClientRest4JsonResponseConsumer<T> extends AbstractAsyncResponseConsumer<HttpResponse> {
	private final long limit;
	private final JsonResponseConsumerFactory<T> factory;
	private final JsonResponseConsumerScope scope;
	private HttpResponse response;
	private JsonResponseConsumer<T> body;
	private JsonGzipDecoder gzip;
	private boolean gzipFailed;
	private ByteBuffer input = ByteBuffer.allocate( JsonTransportSupport.DEFAULT_INPUT_CAPACITY );


	ClientRest4JsonResponseConsumer(long limit, JsonResponseConsumerScope scope,
			JsonResponseConsumerFactory<T> factory) {
		this.factory = factory;
		this.limit = limit;
		this.scope = scope;
		scope.register( this::releaseResources );
	}

	@Override
	protected synchronized void onResponseReceived(HttpResponse response) {
		if ( scope.isCancelled() ) {
			cancel();
			return;
		}
		this.response = response;
	}

	@Override
	protected synchronized void onEntityEnclosed(HttpEntity entity, ContentType contentType) throws IOException {
		if ( scope.isCancelled() || response == null || isDone() ) {
			cancel();
			return;
		}

		Charset charset = contentType == null || contentType.getCharset() == null
				? StandardCharsets.UTF_8
				: contentType.getCharset();
		body = factory.create( charset, limit );
		int status = response.getStatusLine().getStatusCode();
		if ( status != 204
				&& status != 304
				&& entity.getContentEncoding() != null && "gzip".equalsIgnoreCase( entity.getContentEncoding().getValue() ) ) {
			gzip = new JsonGzipDecoder( body );
			// Replay is already decoded. Prevent the REST client from decompressing it a second time.
			response.removeHeaders( "Content-Encoding" );
			response.removeHeaders( "Content-Length" );
		}
		response.setEntity( new Entity<T>( body ) );
	}

	@Override
	protected synchronized void onContentReceived(ContentDecoder decoder, IOControl ioControl) throws IOException {
		if ( input == null ) {
			return;
		}
		int count;
		do {
			input.clear();
			count = decoder.read( input );
			if ( count > 0 ) {

				input.flip();
				if ( gzip != null ) {
					try {
						gzip.feed( input );
					}
					catch (RuntimeException e) {
						// Size failures are terminal, even after an earlier deferred syntax failure.
						if ( limit > 0 && body.inputBytes() > limit ) {
							throw ElasticsearchClientLog.INSTANCE.responseInputLimitExceeded( limit, e );
						}
						body.failedParsing( e );
						gzip.close();
						gzip = null;
						gzipFailed = true;
					}
				}
				else if ( !gzipFailed ) {
					body.feed( input );
				}
			}
		}
		while ( count > 0 );
	}


	@Override
	protected synchronized HttpResponse buildResult(HttpContext context) {
		if ( scope.isCancelled() ) {
			throw new CancellationException();
		}
		if ( body != null ) {
			if ( gzip != null ) {
				try {
					gzip.endOfInput();
				}
				catch (RuntimeException e) {
					body.failedParsing( e );
				}
			}
			body.complete();
		}
		return response;
	}

	@Override
	protected synchronized void releaseResources() {
		if ( gzip != null ) {
			gzip.close();
			gzip = null;
		}
		if ( body != null ) {
			body.close();
			body = null;
		}
		input = null;
		response = null;
	}

	static final class Entity<T> extends AbstractHttpEntity {
		private final JsonResponseConsumer<T> body;

		Entity(JsonResponseConsumer<T> body) {
			this.body = body;
			setContentType( "application/json; charset=UTF-8" );
		}

		JsonResponseConsumer<T> body() {
			return body;
		}

		@Override
		public boolean isRepeatable() {
			return true;
		}

		@Override
		public long getContentLength() {
			return -1;
		}

		@Override
		public InputStream getContent() {
			return body.openDiagnosticStream();
		}

		@Override
		public void writeTo(OutputStream output) throws IOException {
			try ( InputStream stream = getContent() ) {
				stream.transferTo( output );
			}
		}

		@Override
		public boolean isStreaming() {
			return false;
		}
	}
}
