package org.hibernate.search.util.json.example;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparation;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.WorkerExecutor;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;

/** Example of the Search asynchronous preparation SPI. Caller owns Vert.x, the HTTP client and preparation resources. */
public final class SignedJsonClient {
	private final Vertx vertx;
	private final HttpClient http;
	private final ElasticsearchRequestPreparation preparation;
	private final Consumer<HttpClientRequest> configureRequest;
	private final WorkerExecutor signingWorkers;

	public SignedJsonClient(Vertx vertx, HttpClient http, ElasticsearchRequestPreparation preparation) {
		this( vertx, http, preparation, request -> { } );
	}

	/** Configure headers/authority before signing; this callback must not send the request. */
	public SignedJsonClient(Vertx vertx, HttpClient http, ElasticsearchRequestPreparation preparation,
			Consumer<HttpClientRequest> configureRequest) {
		this( vertx, http, preparation, configureRequest, null );
	}

	/** Optional application-owned workers; null selects the existing Vert.x context worker pool. Never closed here. */
	public SignedJsonClient(Vertx vertx, HttpClient http, ElasticsearchRequestPreparation preparation,
			Consumer<HttpClientRequest> configureRequest, WorkerExecutor signingWorkers) {
		this.vertx = vertx;
		this.http = http;
		this.preparation = preparation;
		this.configureRequest = configureRequest;
		this.signingWorkers = signingWorkers;
	}

	public Call send(ElasticsearchRequestPreparation.Request attempt, long timeoutMillis) {
		return send( List.of( attempt ), timeoutMillis );
	}

	/** Finite, caller-selected destinations; retry only 502/503/504 with fresh authentication. */
	public Call send(List<ElasticsearchRequestPreparation.Request> attempts, long timeoutMillis) {
		if ( !Context.isOnEventLoopThread() || timeoutMillis <= 0 || attempts.isEmpty() ) {
			throw new IllegalArgumentException( "Call on a Vert.x event loop with a positive timeout" );
		}
		Call call = new Call( List.copyOf( attempts ), timeoutMillis );
		call.start();
		return call;
	}

	public final class Call implements ElasticsearchRequestPreparation.Execution {
		private final Context context = Vertx.currentContext();
		private final List<ElasticsearchRequestPreparation.Request> attempts;
		private int attemptIndex;
		private long generation;
		private final long deadline;
		private final long timer;
		private final AtomicBoolean stopped = new AtomicBoolean();
		private final Promise<Integer> result = Promise.promise();
		private final ByteBuffer scratch = ByteBuffer.allocate( 8192 );
		private HttpClientRequest request;
		private JsonWriteCursor cursor;

		private Call(List<ElasticsearchRequestPreparation.Request> attempts, long timeoutMillis) {
			this.attempts = attempts;
			deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos( timeoutMillis );
			timer = vertx.setTimer( timeoutMillis, ignored -> fail( new TimeoutException( "Request deadline exceeded" ) ) );
		}

		public Future<Integer> result() {
			return result.future();
		}

		/** May be called from any thread. Never closes a cursor owned by the signing worker. */
		public void cancel() {
			stopped.set( true );
			context.runOnContext( ignored -> fail( new CancellationException( "Request cancelled" ) ) );
		}

		@Override
		public <T> CompletionStage<T> blocking(Callable<T> action) {
			// Both choices are supplied/configured by the application. Search creates no execution pool.
			Callable<T> checked = () -> {
				checkActive();
				return action.call();
			};
			return ( signingWorkers == null ? context.executeBlocking( checked, false )
					: signingWorkers.executeBlocking( checked, false ) ).toCompletionStage();
		}

		@Override
		public void checkActive() {
			if ( stopped.get() ) {
				throw new CancellationException( "Request cancelled" );
			}
			if ( System.nanoTime() - deadline >= 0 ) {
				throw new CompletionException( new TimeoutException( "Request deadline exceeded" ) );
			}
		}

		private void start() {
			long current = ++generation;
			ElasticsearchRequestPreparation.Request attempt = attempts.get( attemptIndex );
			try {
				checkActive();
				RequestOptions options = new RequestOptions().setAbsoluteURI( attempt.uri().toString() )
						.setMethod( HttpMethod.valueOf( attempt.method() ) ).setFollowRedirects( false );
				http.request( options ).onSuccess( outgoing -> {
					if ( !active( current ) ) {
						outgoing.reset();
						return;
					}
					request = outgoing;
					try {
						checkActive();
						outgoing.exceptionHandler( failure -> fail( current, failure ) );
						outgoing.putHeader( "Content-Type", "application/json" );
						attempt.headers().forEach( outgoing::putHeader );
						configureRequest.accept( outgoing );
						if ( attempt.body() != null && attempt.body().contentLength() < 0 ) {
							outgoing.setChunked( true );
						}
						else {
							outgoing.putHeader( "Content-Length", Long.toString( attempt.body() == null ? 0 : attempt.body().contentLength() ) );
						}
						// Snapshot on the event loop: the signing worker never touches a mutable Vert.x request.
						ElasticsearchRequestPreparation.Request snapshot = snapshot( outgoing, attempt );
						Future.fromCompletionStage( preparation.prepare( snapshot, this ), context )
								.onSuccess( prepared -> prepared( current, snapshot, prepared ) )
								.onFailure( failure -> fail( current, failure ) );
					}
					catch (RuntimeException e) {
						fail( current, e );
					}
				} ).onFailure( failure -> fail( current, failure ) );
			}
			catch (RuntimeException e) {
				fail( current, e );
			}
		}

		private ElasticsearchRequestPreparation.Request snapshot(HttpClientRequest outgoing, ElasticsearchRequestPreparation.Request attempt) {
			String scheme = attempt.uri().getScheme();
			// HTTP/1 Host overrides the default authority. Freeze it explicitly for signing and sending.
			String authority = outgoing.headers().get( "Host" );
			if ( authority == null ) {
				authority = outgoing.authority().toString( "https".equalsIgnoreCase( scheme ) );
			}
			outgoing.putHeader( "Host", authority );
			Map<String, List<String>> headers = new LinkedHashMap<>();
			for ( String name : outgoing.headers().names() ) {
				headers.put( name, List.copyOf( outgoing.headers().getAll( name ) ) );
			}
			URI effectiveUri = URI.create( scheme + "://" + authority + outgoing.getURI() );
			return new ElasticsearchRequestPreparation.Request( effectiveUri, outgoing.getMethod().name(), headers, attempt.body() );
		}

		private void prepared(long current, ElasticsearchRequestPreparation.Request snapshot,
				ElasticsearchRequestPreparation.Result prepared) {
			if ( !active( current ) ) {
				return;
			}
			try {
				checkActive();
				// Results replace the complete snapshot, including removal of stale authentication headers.
				request.headers().clear();
				prepared.headers().forEach( request::putHeader );
				cursor = snapshot.body() == null ? null : snapshot.body().openCursor();
				request.response().onSuccess( response -> {
					response.exceptionHandler( failure -> fail( current, failure ) );
					response.handler( ignored -> { } );
					response.endHandler( ignored -> responseComplete( current, response.statusCode() ) );
				} ).onFailure( failure -> fail( current, failure ) );
				if ( cursor == null ) {
					request.end().onFailure( failure -> fail( current, failure ) );
				}
				else {
					pump( current );
				}
			}
			catch (RuntimeException e) {
				fail( current, e );
			}
		}

		private void responseComplete(long current, int status) {
			if ( !active( current ) ) {
				return;
			}
			if ( ( status == 502 || status == 503 || status == 504 ) && attemptIndex + 1 < attempts.size() ) {
				closeCursor();
				HttpClientRequest previous = request;
				request = null;
				++generation; // Ignore late writes/errors from the previous attempt, including reset callbacks.
				previous.reset();
				++attemptIndex;
				start();
			}
			else {
				succeed( status );
			}
		}

		private boolean active(long current) {
			return !stopped.get() && current == generation;
		}

		private void fail(long current, Throwable failure) {
			if ( current == generation ) {
				fail( failure );
			}
		}

		private void pump(long current) {
			if ( !active( current ) ) {
				return;
			}
			try {
				checkActive();
				if ( cursor.isComplete() ) {
					closeCursor();
					request.end().onFailure( failure -> fail( current, failure ) );
					return;
				}
				scratch.clear();
				int count = cursor.write( scratch );
				Buffer output = Buffer.buffer( count ).appendBytes( scratch.array(), 0, count );
				request.write( output ).onSuccess( ignored -> context.runOnContext( next -> pump( current ) ) )
						.onFailure( failure -> fail( current, failure ) );
			}
			catch (RuntimeException e) {
				fail( current, e );
			}
		}

		private void succeed(int status) {
			if ( System.nanoTime() - deadline >= 0 ) {
				fail( new TimeoutException( "Request deadline exceeded" ) );
				return;
			}
			if ( !stopped.compareAndSet( false, true ) ) {
				return;
			}
			vertx.cancelTimer( timer );
			closeCursor();
			result.tryComplete( status );
		}

		private void fail(Throwable failure) {
			if ( result.future().isComplete() ) {
				return;
			}
			stopped.set( true );
			vertx.cancelTimer( timer );
			closeCursor();
			while ( failure instanceof CompletionException && failure.getCause() != null ) {
				failure = failure.getCause();
			}
			result.tryFail( failure );
			if ( request != null ) {
				request.reset();
			}
		}

		private void closeCursor() {
			if ( cursor != null ) {
				cursor.close();
				cursor = null;
			}
		}
	}
}
