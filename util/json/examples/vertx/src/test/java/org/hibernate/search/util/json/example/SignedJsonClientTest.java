package org.hibernate.search.util.json.example;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.search.backend.elasticsearch.client.common.authentication.spi.ElasticsearchRequestPreparation;
import org.hibernate.search.backend.elasticsearch.aws.spi.ElasticsearchAwsRequestSigning;
import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.internal.spi.JsonParsing;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.net.HostAndPort;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner;
import software.amazon.awssdk.http.auth.spi.signer.HttpSigner;

class SignedJsonClientTest {
	private static final Clock CLOCK = Clock.fixed( Instant.parse( "2026-10-16T12:00:00Z" ), ZoneOffset.UTC );
	private static final AwsBasicCredentials CREDENTIALS = AwsBasicCredentials.create( "example-key", "example-secret" );
	private final Vertx vertx = Vertx.vertx();
	private final HttpClient http = vertx.createHttpClient();
	private final AtomicInteger unexpectedRequests = new AtomicInteger();

	@AfterEach
	void close() throws Exception {
		await( http.close() );
		await( vertx.close() );
	}

	@ParameterizedTest
	@ValueSource(strings = { "es", "aoss" })
	void signsOffLoopAndStreamsTheSignedBytesWithIndependentReplay(String service) throws Exception {
		JsonBody original = JsonBodies.of( new JsonParsing().parse(
				"{\"message\":\"" + "é😀".repeat( 50_000 ) + "\",\"number\":9007199254740993}" ) );
		TrackedBody body = new TrackedBody( original );
		AtomicInteger requests = new AtomicInteger();
		CompletableFuture<byte[]> received = new CompletableFuture<>();
		CompletableFuture<String> authorization = new CompletableFuture<>();
		var server = await( vertx.createHttpServer().requestHandler( request -> {
			requests.incrementAndGet();
			MessageDigest digest;
			try {
				digest = MessageDigest.getInstance( "SHA-256" );
			}
			catch (Exception e) {
				throw new IllegalStateException( e );
			}
			ByteArrayOutputStream wire = new ByteArrayOutputStream();
			request.handler( chunk -> {
				byte[] bytes = chunk.getBytes();
				digest.update( bytes );
				wire.writeBytes( bytes ); // Test verification only, not client buffering.
			} );
			request.endHandler( ignored -> {
				received.complete( wire.toByteArray() );
				String receivedHash = HexFormat.of().formatHex( digest.digest() );
				if ( !receivedHash.equals( request.getHeader( "x-amz-content-sha256" ) ) ) {
					authorization.completeExceptionally( new AssertionError( "Wire bytes differ from signed bytes" ) );
					request.response().setStatusCode( 400 ).end();
				}
				else {
					authorization.complete( request.getHeader( "Authorization" ) );
					request.response().setStatusCode( 204 ).end();
				}
			} );
		} ).listen( 0, "127.0.0.1" ) );
		URI uri = URI.create( "http://127.0.0.1:" + server.actualPort() + "/index/_bulk?refresh=true" );
		CountDownLatch resolving = new CountDownLatch( 1 );
		CountDownLatch release = new CountDownLatch( 1 );
		AwsCredentialsProvider credentials = () -> {
			assertTrue( Context.isOnWorkerThread() );
			assertFalse( Context.isOnEventLoopThread() );
			resolving.countDown();
			waitFor( release );
			return CREDENTIALS;
		};
		var client = new SignedJsonClient( vertx, http, new ElasticsearchAwsRequestSigning( credentials, "us-east-1", service, CLOCK ) );
		SignedJsonClient.Call call = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( uri, body ), 10_000 ) );
		try {
			assertTrue( resolving.await( 5, TimeUnit.SECONDS ) );
			assertTrue( onLoop( Context::isOnEventLoopThread ) ); // Still responsive while credentials are blocked.
			assertEquals( 0, requests.get() );
		}
		finally {
			release.countDown();
		}
		assertEquals( 204, await( call.result() ) );
		String expected;
		try ( var stream = original.openStream() ) {
			var expectedRequest = SdkHttpRequest.builder().uri( uri ).method( SdkHttpMethod.POST )
					.putHeader( "Content-Type", "application/json" );
			var signed = AwsV4HttpSigner.create().sign( r -> r.identity( CREDENTIALS )
					.request( expectedRequest.build() )
					.payload( () -> stream )
					.putProperty( AwsV4HttpSigner.REGION_NAME, "us-east-1" )
					.putProperty( AwsV4HttpSigner.SERVICE_SIGNING_NAME, service )
					.putProperty( HttpSigner.SIGNING_CLOCK, CLOCK ) );
			expected = signed.request().firstMatchingHeader( "Authorization" ).orElseThrow();
		}
		assertEquals( expected, authorization.get( 5, TimeUnit.SECONDS ) );
		byte[] wire = received.get( 5, TimeUnit.SECONDS );
		try ( var decoded = new ByteArrayInputStream( wire );
				var json = original.openStream() ) {
			assertArrayEquals( json.readAllBytes(), decoded.readAllBytes() );
		}
		assertEquals( 1, body.workerCursors.get() );
		assertEquals( 1, body.eventLoopCursors.get() );
		assertEquals( 2, body.closed.get() );
		assertTrue( body.largestWrite.get() <= JsonBodies.DEFAULT_BUFFER_SIZE );
		// Explicit new attempt/replay, never reset or reuse the old traversal.
		var retry = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( uri, body ), 10_000 ) );
		assertEquals( 204, await( retry.result() ) );
		assertEquals( 2, requests.get() );
		assertEquals( 4, body.closed.get() );
	}

	@Test
	void cancellationDuringSigningClosesOnTheWorkerWithoutStartingTransport() throws Exception {
		URI uri = listeningUri();
		TrackedBody body = new TrackedBody( JsonBodies.of( new JsonParsing().parse( "{\"a\":1}" ) ) );
		body.readEntered = new CountDownLatch( 1 );
		body.readRelease = new CountDownLatch( 1 );
		var client = new SignedJsonClient( vertx, http,
				new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK ) );
		var call = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( uri, body ),
				10_000 ) );
		try {
			assertTrue( body.readEntered.await( 5, TimeUnit.SECONDS ) );
			call.cancel();
			assertThrows( CancellationException.class, () -> await( call.result() ) );
		}
		finally {
			body.readRelease.countDown();
		}
		assertTrue( body.workerClosed.await( 5, TimeUnit.SECONDS ) );
		assertEquals( 0, body.eventLoopCursors.get() );
		assertEquals( 0, unexpectedRequests.get() );
		assertEquals( 1, body.closed.get() );
	}

	@Test
	void deadlineIncludesCredentialLookup() throws Exception {
		URI uri = listeningUri();
		CountDownLatch entered = new CountDownLatch( 1 );
		CountDownLatch release = new CountDownLatch( 1 );
		TrackedBody body = new TrackedBody( JsonBodies.of( new JsonParsing().parse( "{}" ) ) );
		var signer = new ElasticsearchAwsRequestSigning( () -> {
			entered.countDown();
			waitFor( release );
			return CREDENTIALS;
		}, "us-east-1", "es", CLOCK );
		var client = new SignedJsonClient( vertx, http, signer );
		var call = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( uri, body ),
				500 ) );
		try {
			assertTrue( entered.await( 5, TimeUnit.SECONDS ) );
			ExecutionException failure = assertThrows( ExecutionException.class, () -> await( call.result() ) );
			assertInstanceOf( TimeoutException.class, failure.getCause() );
		}
		finally {
			release.countDown();
		}
		assertEquals( 0, body.workerCursors.get() );
		assertEquals( 0, body.eventLoopCursors.get() );
		assertEquals( 0, unexpectedRequests.get() );
	}

	@Test
	void deadlineCheckBeforeTimerCallbackStillReportsTimeout() throws Exception {
		URI uri = listeningUri();
		ElasticsearchRequestPreparation preparation = (request, execution) -> {
			// Deliberately hold this context turn so the timer cannot run before checkActive.
			long until = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos( 600 );
			while ( System.nanoTime() - until < 0 ) {
				LockSupport.parkNanos( until - System.nanoTime() );
			}
			execution.checkActive();
			return CompletableFuture.completedFuture( new ElasticsearchRequestPreparation.Result( request.headers() ) );
		};
		var client = new SignedJsonClient( vertx, http, preparation );
		var call = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( uri, null ), 500 ) );
		ExecutionException failure = assertThrows( ExecutionException.class, () -> await( call.result() ) );
		assertInstanceOf( TimeoutException.class, failure.getCause() );
		assertEquals( 0, unexpectedRequests.get() );
	}

	@Test
	void credentialFailurePropagatesWithoutOpeningBody() throws Exception {
		URI uri = listeningUri();
		TrackedBody body = new TrackedBody( JsonBodies.of( new JsonParsing().parse( "{}" ) ) );
		var signer = new ElasticsearchAwsRequestSigning( () -> {
			throw new IllegalStateException( "credential lookup failed" );
		}, "us-east-1", "es", CLOCK );
		var client = new SignedJsonClient( vertx, http, signer );
		var call = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( uri, body ),
				10_000 ) );
		ExecutionException failure = assertThrows( ExecutionException.class, () -> await( call.result() ) );
		assertEquals( "credential lookup failed", failure.getCause().getMessage() );
		assertEquals( 0, body.workerCursors.get() );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void signsEffectiveAuthorityMethodUriAndUserHeaders(boolean hostHeader) throws Exception {
		CompletableFuture<Void> verified = new CompletableFuture<>();
		var server = await( vertx.createHttpServer().requestHandler( request -> request.body().onSuccess( wire -> {
			try {
				assertEquals( "signed.example:9443", request.authority().toString( false ) );
				assertEquals( HttpMethod.PUT, request.method() );
				assertEquals( "/prefix/a%2Fb?value=a%20b&value=c%2Bd", request.uri() );
				assertEquals( "application/x-ndjson", request.getHeader( "Content-Type" ) );
				assertEquals( List.of( "one", "two" ), request.headers().getAll( "X-Example" ) );
				verifyWireSignature( request, wire.getBytes() );
				verified.complete( null );
				request.response().setStatusCode( 204 ).end();
			}
			catch (Throwable e) {
				verified.completeExceptionally( e );
				request.response().setStatusCode( 400 ).end();
			}
		} ) ).listen( 0, "127.0.0.1" ) );
		AtomicReference<ElasticsearchRequestPreparation.Request> signedSnapshot = new AtomicReference<>();
		var aws = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK );
		ElasticsearchRequestPreparation authentication = (snapshot, execution) -> {
			assertTrue( Context.isOnEventLoopThread() );
			signedSnapshot.set( snapshot );
			return aws.prepare( snapshot, execution );
		};
		var client = new SignedJsonClient( vertx, http, authentication, request -> {
			if ( hostHeader ) {
				request.putHeader( "Host", "signed.example:9443" );
			}
			else {
				request.authority( HostAndPort.create( "signed.example", 9443 ) );
			}
			request.setMethod( HttpMethod.PUT );
			request.setURI( "/prefix/a%2Fb?value=a%20b&value=c%2Bd" );
			request.putHeader( "Content-Type", "application/x-ndjson" );
			request.putHeader( "X-Example", List.<String>of( "one", "two" ) );
		} );
		var body = JsonBodies.of( new JsonParsing().parse( "{\"query\":{}}" ) );
		URI destination = URI.create( "http://127.0.0.1:" + server.actualPort() + "/original" );
		var call = onLoop( () -> client.send( new ElasticsearchRequestPreparation.Request( destination, "POST",
				Map.of( "Content-Type", List.of( "application/json" ), "X-Example", List.of( "original" ) ), body ), 10_000 ) );
		assertEquals( 204, await( call.result() ) );
		verified.get( 5, TimeUnit.SECONDS );
		assertEquals( "signed.example", signedSnapshot.get().uri().getHost() );
		assertThrows( UnsupportedOperationException.class, () -> signedSnapshot.get().headers().clear() );
	}

	@Test
	void retryOnAnotherDestinationCreatesFreshSignatureAndCursors() throws Exception {
		List<String> signatures = new ArrayList<>();
		List<ElasticsearchRequestPreparation.Request> snapshots = new ArrayList<>();
		CompletableFuture<Void> verified = new CompletableFuture<>();
		List<URI> destinations = new ArrayList<>();
		for ( int status : new int[] { 503, 204 } ) {
			var server = await( vertx.createHttpServer().requestHandler( request -> request.body().onSuccess( wire -> {
				try {
					verifyWireSignature( request, wire.getBytes() );
					signatures.add( request.getHeader( "Authorization" ) );
					if ( status == 204 ) {
						verified.complete( null );
					}
					request.response().setStatusCode( status ).end();
				}
				catch (Throwable e) {
					verified.completeExceptionally( e );
					request.response().setStatusCode( 400 ).end();
				}
			} ) ).listen( 0, "127.0.0.1" ) );
			destinations.add( URI.create( "http://127.0.0.1:" + server.actualPort() + "/_bulk" ) );
		}
		var body = new TrackedBody( JsonBodies.of( new JsonParsing().parse( "{\"value\":\"é😀\"}" ) ) );
		var aws = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK );
		ElasticsearchRequestPreparation authentication = (snapshot, execution) -> {
			snapshots.add( snapshot );
			return aws.prepare( snapshot, execution );
		};
		var client = new SignedJsonClient( vertx, http, authentication );
		var attempts = destinations.stream().map( uri -> new ElasticsearchRequestPreparation.Request( uri, body ) ).toList();
		var call = onLoop( () -> client.send( attempts, 10_000 ) );
		assertEquals( 204, await( call.result() ) );
		verified.get( 5, TimeUnit.SECONDS );
		assertEquals( destinations, snapshots.stream().map( ElasticsearchRequestPreparation.Request::uri ).toList() );
		assertEquals( 2, signatures.size() );
		assertFalse( signatures.get( 0 ).equals( signatures.get( 1 ) ) );
		assertEquals( 2, body.workerCursors.get() );
		assertEquals( 2, body.eventLoopCursors.get() );
		assertEquals( 4, body.closed.get() );
	}

	@ParameterizedTest
	@ValueSource(booleans = { false, true })
	void cancellationOrDeadlineDuringRetryPreventsSecondSend(boolean cancel) throws Exception {
		AtomicInteger requests = new AtomicInteger();
		var server = await( vertx.createHttpServer().requestHandler( request -> request.body().onSuccess( wire -> {
			requests.incrementAndGet();
			request.response().setStatusCode( 503 ).end();
		} ) ).listen( 0, "127.0.0.1" ) );
		URI uri = URI.create( "http://127.0.0.1:" + server.actualPort() + "/_bulk" );
		CountDownLatch secondLookup = new CountDownLatch( 1 );
		CountDownLatch release = new CountDownLatch( 1 );
		CountDownLatch signingDone = new CountDownLatch( 1 );
		AtomicInteger lookups = new AtomicInteger();
		var aws = new ElasticsearchAwsRequestSigning( () -> {
			if ( lookups.incrementAndGet() == 2 ) {
				secondLookup.countDown();
				waitFor( release );
			}
			return CREDENTIALS;
		}, "us-east-1", "es", CLOCK );
		AtomicInteger preparations = new AtomicInteger();
		ElasticsearchRequestPreparation authentication = (snapshot, execution) -> {
			int number = preparations.incrementAndGet();
			return aws.prepare( snapshot, execution ).whenComplete( (result, failure) -> {
				if ( number == 2 ) {
					signingDone.countDown();
				}
			} );
		};
		var body = new TrackedBody( JsonBodies.of( new JsonParsing().parse( "{}" ) ) );
		var attempt = new ElasticsearchRequestPreparation.Request( uri, body );
		var client = new SignedJsonClient( vertx, http, authentication );
		var call = onLoop( () -> client.send( List.of( attempt, attempt ), cancel ? 10_000 : 1_000 ) );
		try {
			assertTrue( secondLookup.await( 5, TimeUnit.SECONDS ) );
			if ( cancel ) {
				call.cancel();
				assertThrows( CancellationException.class, () -> await( call.result() ) );
			}
			else {
				ExecutionException failure = assertThrows( ExecutionException.class, () -> await( call.result() ) );
				assertInstanceOf( TimeoutException.class, failure.getCause() );
			}
		}
		finally {
			release.countDown();
		}
		assertTrue( signingDone.await( 5, TimeUnit.SECONDS ) );
		onLoop( () -> null );
		assertEquals( 1, requests.get() );
		assertEquals( 1, body.workerCursors.get() );
		assertEquals( 1, body.eventLoopCursors.get() );
		assertEquals( 2, body.closed.get() );
	}

	/** Reconstruct the signed request from received HTTP metadata and bytes, not the client snapshot. */
	private static void verifyWireSignature(HttpServerRequest request, byte[] wire) throws Exception {
		String authorization = request.getHeader( "Authorization" );
		String signedNames = authorization.split( "SignedHeaders=" )[1].split( "," )[0];
		String authority = request.authority().toString( false );
		var reference = SdkHttpRequest.builder().uri( URI.create( "http://" + authority + request.uri() ) )
				.method( SdkHttpMethod.fromValue( request.method().name() ) );
		for ( String name : signedNames.split( ";" ) ) {
			reference.putHeader( name, "host".equals( name ) ? List.of( authority ) : request.headers().getAll( name ) );
		}
		var signed = AwsV4HttpSigner.create().sign( r -> r.identity( CREDENTIALS ).request( reference.build() )
				.payload( () -> new ByteArrayInputStream( wire ) )
				.putProperty( AwsV4HttpSigner.REGION_NAME, "us-east-1" )
				.putProperty( AwsV4HttpSigner.SERVICE_SIGNING_NAME, "es" )
				.putProperty( HttpSigner.SIGNING_CLOCK, CLOCK ) );
		assertEquals( authorization, signed.request().firstMatchingHeader( "Authorization" ).orElseThrow() );
		assertEquals( HexFormat.of().formatHex( MessageDigest.getInstance( "SHA-256" ).digest( wire ) ),
				request.getHeader( "x-amz-content-sha256" ) );
	}

	@Test
	void absentGetBodyAndCompleteHeaderReplacementRemoveStaleSessionAuthentication() throws Exception {
		var verified = new CompletableFuture<Void>();
		var server = await( vertx.createHttpServer().requestHandler( request -> request.bodyHandler( body -> {
			try {
				assertEquals( HttpMethod.GET, request.method() );
				assertEquals( 0, body.length() );
				assertEquals( null, request.getHeader( "x-amz-security-token" ) );
				verifyWireSignature( request, body.getBytes() );
				verified.complete( null );
				request.response().setStatusCode( 204 ).end();
			}
			catch (Throwable failure) {
				verified.completeExceptionally( failure );
				request.response().setStatusCode( 400 ).end();
			}
		} ) ).listen( 0, "127.0.0.1" ) );
		var signing = new ElasticsearchAwsRequestSigning( () -> CREDENTIALS, "us-east-1", "es", CLOCK );
		var client = new SignedJsonClient( vertx, http, signing );
		var request = new ElasticsearchRequestPreparation.Request(
				URI.create( "http://127.0.0.1:" + server.actualPort() + "/empty" ), "GET",
				Map.of( "Authorization", List.of( "expired" ), "x-amz-security-token", List.of( "expired-token" ) ), null );
		assertEquals( 204, await( onLoop( () -> client.send( request, 10_000 ) ).result() ) );
		verified.get( 5, TimeUnit.SECONDS );
	}

	@Test
	void integrationCanSelectAnExistingWorkerExecutorAndKeepsItsOwnership() throws Exception {
		var workers = vertx.createSharedWorkerExecutor( "selected-signing", 1 );
		try {
			var server = await( vertx.createHttpServer().requestHandler( request -> request.bodyHandler(
					ignored -> request.response().setStatusCode( 204 ).end() ) ).listen( 0, "127.0.0.1" ) );
			var signer = new ElasticsearchAwsRequestSigning( () -> {
				assertTrue( Thread.currentThread().getName().startsWith( "selected-signing" ) );
				return CREDENTIALS;
			}, "us-east-1", "es", CLOCK );
			var client = new SignedJsonClient( vertx, http, signer, request -> { }, workers );
			var request = new ElasticsearchRequestPreparation.Request(
					URI.create( "http://127.0.0.1:" + server.actualPort() + "/" ), null );
			assertEquals( 204, await( onLoop( () -> client.send( request, 10_000 ) ).result() ) );
			assertEquals( "still owned", await( workers.executeBlocking( () -> "still owned", false ) ) );
		}
		finally {
			await( workers.close() );
		}
	}

	private URI listeningUri() throws Exception {
		var server = await( vertx.createHttpServer().requestHandler( request -> {
			unexpectedRequests.incrementAndGet();
			request.response().end();
		} ).listen( 0, "127.0.0.1" ) );
		return URI.create( "http://127.0.0.1:" + server.actualPort() + "/" );
	}

	private <T> T onLoop(Callable<T> action) throws Exception {
		CompletableFuture<T> result = new CompletableFuture<>();
		vertx.runOnContext( ignored -> {
			try {
				result.complete( action.call() );
			}
			catch (Throwable e) {
				result.completeExceptionally( e );
			}
		} );
		return result.get( 10, TimeUnit.SECONDS );
	}

	private static <T> T await(Future<T> future) throws Exception {
		return future.toCompletionStage().toCompletableFuture().get( 10, TimeUnit.SECONDS );
	}

	private static void waitFor(CountDownLatch latch) {
		try {
			if ( !latch.await( 5, TimeUnit.SECONDS ) ) {
				throw new IllegalStateException( "Test latch timed out" );
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException( e );
		}
	}

	private static final class TrackedBody implements JsonBody {
		private final JsonBody delegate;
		private final AtomicInteger workerCursors = new AtomicInteger();
		private final AtomicInteger eventLoopCursors = new AtomicInteger();
		private final AtomicInteger closed = new AtomicInteger();
		private final AtomicInteger largestWrite = new AtomicInteger();
		private final CountDownLatch workerClosed = new CountDownLatch( 1 );
		private CountDownLatch readEntered;
		private CountDownLatch readRelease;

		private TrackedBody(JsonBody delegate) {
			this.delegate = delegate;
		}

		@Override
		public JsonWriteCursor openCursor() {
			boolean worker = Context.isOnWorkerThread();
			assertTrue( worker || Context.isOnEventLoopThread() );
			( worker ? workerCursors : eventLoopCursors ).incrementAndGet();
			Thread owner = Thread.currentThread();
			JsonWriteCursor cursor = delegate.openCursor();
			return new JsonWriteCursor() {
				private boolean done;

				public int write(ByteBuffer destination) {
					assertSame( owner, Thread.currentThread() );
					if ( worker && readEntered != null ) {
						readEntered.countDown();
						waitFor( readRelease );
					}
					int count = cursor.write( destination );
					largestWrite.accumulateAndGet( count, Math::max );
					return count;
				}

				public boolean isComplete() {
					return cursor.isComplete();
				}

				public void close() {
					assertSame( owner, Thread.currentThread() );
					if ( !done ) {
						done = true;
						cursor.close();
						closed.incrementAndGet();
						if ( worker ) {
							workerClosed.countDown();
						}
					}
				}
			};
		}
	}
}
