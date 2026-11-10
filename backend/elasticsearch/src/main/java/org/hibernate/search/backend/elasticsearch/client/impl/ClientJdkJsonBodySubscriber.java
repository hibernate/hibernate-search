package org.hibernate.search.backend.elasticsearch.client.impl;

import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonGzipDecoder;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonTransportSupport;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.json.spi.JsonResponseConsumer;
import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;


/** Parses on the HTTP client's callback thread, requesting one consumed batch at a time. */
final class ClientJdkJsonBodySubscriber<T> implements HttpResponse.BodySubscriber<T> {
	private final CompletableFuture<T> result = new CompletableFuture<>();
	private final JsonResponseConsumer<T> body;
	private JsonGzipDecoder gzip;
	private final int statusCode;
	private final long limit;
	private Flow.Subscription subscription;
	private boolean terminated;
	private long inputBytes;


	ClientJdkJsonBodySubscriber(JsonResponseConsumerFactory<T> factory,
			Charset charset, int statusCode, boolean compressed) {
		this( factory, charset, statusCode, compressed, JsonTransportSupport.DEFAULT_LIMIT );
	}

	ClientJdkJsonBodySubscriber(JsonResponseConsumerFactory<T> factory,
			Charset charset, int statusCode, boolean compressed, long limit) {
		this.statusCode = statusCode;
		this.limit = limit;
		// Bound both transport bytes below and decoded bytes in the response body.
		body = factory.create( charset, limit );
		if ( compressed && statusCode != 204 && statusCode != 304 ) {
			gzip = new JsonGzipDecoder( body );
		}
		result.whenComplete( (value, failure) -> {
			if ( result.isCancelled() ) {
				cancel();
			}
		} );
	}

	@Override
	public CompletionStage<T> getBody() {
		return result;
	}

	@Override
	public synchronized void onSubscribe(Flow.Subscription incoming) {
		if ( subscription != null || terminated ) {
			incoming.cancel();
			return;
		}
		subscription = incoming;
		incoming.request( 1 );
	}

	@Override
	public synchronized void onNext(List<ByteBuffer> buffers) {
		if ( terminated ) {
			return;
		}
		try {
			for ( ByteBuffer buffer : buffers ) {
				inputBytes += buffer.remaining();
				if ( limit > 0 && inputBytes > limit ) {
					throw ElasticsearchClientLog.INSTANCE.responseInputLimitExceeded( limit );
				}
				if ( gzip == null ) {
					body.feed( buffer );
				}
				else {
					gzip.feed( buffer );
				}
				body.throwIfFailed();
			}
			subscription.request( 1 );
		}
		catch (RuntimeException e) {
			cancel();
			result.completeExceptionally( ElasticsearchClientLog.INSTANCE.failedToParseElasticsearchResponse(
					statusCode, null, e.getMessage(), e ) );
		}
	}

	@Override
	public synchronized void onError(Throwable failure) {
		if ( terminated ) {
			return;
		}
		cancel();
		result.completeExceptionally( failure );
	}

	@Override
	public synchronized void onComplete() {
		if ( terminated ) {
			return;
		}
		try {
			if ( gzip != null ) {
				gzip.endOfInput();
			}
			body.complete();
			T tree = body.result();
			terminated = true;
			close();
			result.complete( tree );
		}
		catch (RuntimeException e) {
			cancel();
			result.completeExceptionally( ElasticsearchClientLog.INSTANCE.failedToParseElasticsearchResponse(
					statusCode, null, e.getMessage(), e ) );
		}
	}

	private void close() {
		body.close();
		if ( gzip != null ) {
			gzip.close();
			gzip = null;
		}
	}

	synchronized void cancel() {
		terminated = true;
		close();
		if ( subscription != null ) {
			subscription.cancel();
		}
	}
}
