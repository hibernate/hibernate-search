package org.hibernate.search.integrationtest.backend.elasticsearch.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.search.util.impl.integrationtest.backend.elasticsearch.extension.TestElasticsearchClient.getDiscoveredClientFactory;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPOutputStream;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchBackendSettings;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonClientImplementor;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonRequest;
import org.hibernate.search.backend.elasticsearch.client.common.impl.ElasticsearchJsonResponse;
import org.hibernate.search.backend.elasticsearch.client.common.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonScalarConversions;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.engine.cfg.spi.AllAwareConfigurationPropertySource;
import org.hibernate.search.engine.common.execution.spi.DelegatingSimpleScheduledExecutor;
import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider;
import org.hibernate.search.engine.environment.thread.impl.ThreadPoolProviderImpl;
import org.hibernate.search.util.impl.integrationtest.common.TestConfigurationProvider;
import org.hibernate.search.util.impl.test.http.StreamingHttpServer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Runs unchanged in all four client executions; transfers use latches instead of timing sleeps.
 */
class ElasticsearchJsonTransportIT {

	@RegisterExtension
	private final TestConfigurationProvider configuration = new TestConfigurationProvider();

	private final ThreadPoolProviderImpl threads = new ThreadPoolProviderImpl(
			BeanHolder.of( new EmbeddedThreadProvider( ElasticsearchJsonTransportIT.class.getName() ) ) );

	private final ScheduledExecutorService timeouts = threads.newScheduledExecutor( 1, "Timeout" );

	@AfterEach
	void closeThreads() {
		timeouts.shutdownNow();
		threads.close();
	}

	@Test
	void gzipAndExplicitCharset() throws Exception {
		String scalar = "é😀".repeat( 10_000 );
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try ( GZIPOutputStream gzip = new GZIPOutputStream( bytes ) ) {
			gzip.write( ( "{\"value\":\"" + scalar + "\"}" ).getBytes( StandardCharsets.UTF_16LE ) );
		}
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json; charset=UTF-16LE", "Content-Encoding: gzip\r\n" );
			byte[] compressed = bytes.toByteArray();
			for ( int offset = 0; offset < compressed.length; offset += 5 ) {
				exchange.chunk( Arrays.copyOfRange( compressed, offset, Math.min( offset + 5, compressed.length ) ) );
			}
			exchange.end();
		} );
				var client = client( List.of( server.uri() ), Map.of() ) ) {
			assertThat( JsonScalarConversions
					.asString( client.submit( request( "gzip" ) ).get( 10, TimeUnit.SECONDS ).bodyJson().get( "value" ) ) )
					.isEqualTo( scalar );
		}
	}

	@Test
	void malformedErrorRetryPolicyAndLargeBodyReplay() throws Exception {
		for ( String retryBody : List.of( "<proxy>unavailable", "{\"truncated\":" ) ) {
			AtomicInteger attempts = new AtomicInteger();
			ObjectNode document = new ObjectNode( JsonNodeFactory.instance );
			document.put( "value", "é😀".repeat( 100_000 ) );
			String expected = JsonProviderHelper.create( true ).getCodec()
					.write( document ) + "\n";
			StreamingHttpServer.Handler handler = exchange -> {
				ByteArrayOutputStream upload = new ByteArrayOutputStream();
				exchange.readBody( (bytes, count) -> upload.write( bytes, 0, count ) );
				assertThat( upload.toString( StandardCharsets.UTF_8 ) ).isEqualTo( expected );
				boolean first = attempts.incrementAndGet() == 1;
				exchange.headers( first ? 503 : 200, "application/json", "" );
				exchange.chunk( ( first ? retryBody : "{\"retried\":true}" ).getBytes( StandardCharsets.UTF_8 ) );
				exchange.end();
			};
			try ( var first = new StreamingHttpServer( handler );
					var second = new StreamingHttpServer( handler );
					var client = client( List.of( first.uri(), second.uri() ), Map.of() ) ) {
				var request = ElasticsearchJsonRequest.post().pathComponent( URLEncodedString.fromString( "retry" ) )
						.bodyJson( document ).build();
				if ( isJdk() ) {
					// The existing JDK client does not implement status-based host failover.
					assertThatThrownBy( () -> client.submit( request ).get( 10, TimeUnit.SECONDS ) )
							.hasMessageContaining( "503" );
					assertThat( attempts ).hasValue( 1 );
				}
				else {
					assertThat( JsonScalarConversions
							.asBoolean( client.submit( request ).get( 10, TimeUnit.SECONDS ).bodyJson().get( "retried" ) ) )
							.isTrue();
					assertThat( attempts ).hasValue( 2 );
				}
				first.checkFailure();
				second.checkFailure();
			}
		}
	}

	@Test
	void retryAfterPartialUploadReplaysFromTheBeginning() throws Exception {
		AtomicInteger attempts = new AtomicInteger();
		ObjectNode document = new ObjectNode( JsonNodeFactory.instance );
		document.put( "value", "é😀".repeat( 1_000_000 ) );
		long expectedLength = 6_000_013;
		StreamingHttpServer.Handler handler = exchange -> {
			boolean first = attempts.incrementAndGet() == 1;
			var received = new AtomicLong();
			var prefix = new ByteArrayOutputStream();
			exchange.readBody( (bytes, count) -> {
				if ( first ) {
					// Close after receiving only the first bounded fragment of a multi-megabyte upload.
					assertThat( count ).isBetween( 1, 8192 );
					throw new EOFException( "Fixture interrupts partial upload" );
				}
				if ( received.get() < 12 ) {
					prefix.write( bytes, 0, (int) Math.min( count, 12 - received.get() ) );
				}
				received.addAndGet( count );
			} );
			assertThat( received ).hasValue( expectedLength );
			assertThat( prefix.toString( StandardCharsets.UTF_8 ) ).startsWith( "{\"value\":\"" );
			exchange.headers( 200, "application/json", "" );
			exchange.chunk( "{}".getBytes( StandardCharsets.UTF_8 ) );
			exchange.end();
		};
		try ( var first = new StreamingHttpServer( handler );
				var second = new StreamingHttpServer( handler );
				var client = client( List.of( first.uri(), second.uri() ), Map.of() ) ) {
			var request = ElasticsearchJsonRequest.post().pathComponent( URLEncodedString.fromString( "partial" ) )
					.bodyJson( document ).build();
			if ( isJdk() ) {
				assertThatThrownBy( () -> client.submit( request ).get( 10, TimeUnit.SECONDS ) )
						.hasRootCauseInstanceOf( IOException.class );
			}
			else {
				assertThat( client.submit( request ).get( 10, TimeUnit.SECONDS ).statusCode() ).isEqualTo( 200 );
				assertThat( attempts ).hasValue( 2 );
			}
			first.checkFailure();
			second.checkFailure();
		}
	}

	@Test
	void malformedFinalResponseThenAnotherRequest() throws Exception {
		AtomicInteger requests = new AtomicInteger();
		AtomicInteger firstConnection = new AtomicInteger();
		AtomicInteger secondConnection = new AtomicInteger();
		try ( var server = new StreamingHttpServer( exchange -> {
			boolean first = requests.incrementAndGet() == 1;
			( first ? firstConnection : secondConnection ).set( exchange.connection );
			exchange.headers( first ? 400 : 200, "application/json", "" );
			exchange.chunk( ( first ? "{\"truncated\":" : "{}" ).getBytes( StandardCharsets.UTF_8 ) );
			exchange.end();
		} );
				var client = client( List.of( server.uri() ), Map.of() ) ) {
			assertThatThrownBy( () -> client.submit( request( "malformed" ) ).get( 10, TimeUnit.SECONDS ) )
					.hasMessageContaining( "400" );
			assertThat( client.submit( request( "next" ) ).get( 10, TimeUnit.SECONDS ).statusCode() ).isEqualTo( 200 );
			if ( !isJdk() ) {
				assertThat( secondConnection ).hasValue( firstConnection.get() );
			}
		}
	}

	@Test
	void cancellationAndTimeoutDuringSlowResponse() throws Exception {
		for ( boolean timeout : List.of( false, true ) ) {
			CountDownLatch prefix = new CountDownLatch( 1 );
			CountDownLatch disconnected = new CountDownLatch( 1 );
			CountDownLatch releaseHc5 = new CountDownLatch( 1 );
			boolean requireSocketAbort = isJdk() || isRest4();
			AtomicInteger requests = new AtomicInteger();
			try ( var server = new StreamingHttpServer( exchange -> {
				exchange.headers( 200, "application/json", "" );
				exchange.chunk( "{}".getBytes( StandardCharsets.UTF_8 ) );
				if ( requests.incrementAndGet() == 1 ) {
					prefix.countDown();
					if ( requireSocketAbort ) {
						assertThat( exchange.awaitDisconnect() ).isTrue();
						disconnected.countDown();
						exchange.closeConnection();
					}
					else {
						// The pinned HC5 handles cancel their futures but do not promptly abort this stalled socket.
						assertThat( releaseHc5.await( 10, TimeUnit.SECONDS ) ).isTrue();
						exchange.end();
					}
				}
				else {
					exchange.end();
				}
			} );
					var client = client( List.of( server.uri() ), timeout ? Map.of( "request_timeout", 500 ) : Map.of() ) ) {
				var response = client.submit( request( "pause" ) );
				assertThat( prefix.await( 10, TimeUnit.SECONDS ) ).isTrue();
				if ( timeout ) {
					assertThatThrownBy( () -> response.get( 10, TimeUnit.SECONDS ) ).hasMessageContaining( "timeout" );
				}
				else {
					assertThat( response.cancel( true ) ).isTrue();
					assertThat( response ).isCancelled();
				}
				if ( requireSocketAbort ) {
					assertThat( disconnected.await( 10, TimeUnit.SECONDS ) ).isTrue();
				}
				else {
					releaseHc5.countDown();
				}
				assertThat( client.submit( request( "next" ) ).get( 10, TimeUnit.SECONDS ).statusCode() ).isEqualTo( 200 );
			}
		}
	}

	@Test
	void configuredByteCapBeforeCharsetConversion() throws Exception {
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json; charset=UTF-16LE", "" );
			byte[] whitespace = new byte[8192];
			for ( int i = 0; i < whitespace.length; i += 2 ) {
				whitespace[i] = 32;
			}
			for ( int chunk = 0; chunk < 12_801; ++chunk ) {
				exchange.chunk( whitespace );
			}
			exchange.end();
		} );
				var client = client( List.of( server.uri() ),
						Map.of( ElasticsearchBackendSettings.RESPONSE_BODY_SIZE_LIMIT, 104857600L ) ) ) {
			CompletableFuture<ElasticsearchJsonResponse> response = client.submit( request( "limit" ) );
			assertThatThrownBy( () -> response.get( 30, TimeUnit.SECONDS ) ).hasMessageContaining( "byte limit 104857600" );
		}
	}

	@Test
	@SuppressWarnings("unused")
	void slowUploadAndEarlyRejection() throws Exception {
		ObjectNode document = new ObjectNode( JsonNodeFactory.instance );
		document.put( "value", "é😀".repeat( 1_000_000 ) );
		for ( boolean reject : List.of( false, true ) ) {
			CountDownLatch started = new CountDownLatch( 1 );
			CountDownLatch continueUpload = new CountDownLatch( 1 );
			AtomicInteger uploadBytes = new AtomicInteger();
			try ( var server = new StreamingHttpServer( exchange -> {
				if ( reject ) {
					exchange.headers( 413, "application/json", "Connection: close\r\n" );
					exchange.chunk( "{}".getBytes( StandardCharsets.UTF_8 ) );
					exchange.end();
					exchange.closeConnection();
					started.countDown();
				}
				else {
					exchange.readBody( (bytes, count) -> {
						if ( uploadBytes.getAndAdd( count ) == 0 ) {
							started.countDown();
							assertThat( continueUpload.await( 10, TimeUnit.SECONDS ) ).isTrue();
						}
					} );
					exchange.headers( 200, "application/json", "" );
					exchange.chunk( "{}".getBytes( StandardCharsets.UTF_8 ) );
					exchange.end();
				}
			} );
					var client = client( List.of( server.uri() ), Map.of() ) ) {
				var response = client.submit( ElasticsearchJsonRequest.post()
						.pathComponent( URLEncodedString.fromString( "upload" ) ).bodyJson( document ).build() );
				try {
					assertThat( started.await( 10, TimeUnit.SECONDS ) ).isTrue();
					if ( !reject ) {
						assertThat( response ).isNotDone();
						assertThat( uploadBytes.get() ).isBetween( 1, 8192 );
					}
				}
				finally {
					continueUpload.countDown();
				}
				if ( reject ) {
					// Early connection closure can win the race with the client's 413 delivery.
					try {
						assertThat( response.get( 10, TimeUnit.SECONDS ).statusCode() ).isEqualTo( 413 );
					}
					catch (ExecutionException e) {
						assertThat( e.getCause() ).isNotNull();
					}
				}
				else {
					assertThat( response.get( 10, TimeUnit.SECONDS ).statusCode() ).isEqualTo( 200 );
					assertThat( uploadBytes ).hasValue( 6_000_013 );
				}
			}
		}
	}

	private boolean isJdk() {
		return getDiscoveredClientFactory( configuration.createBeanResolverForTest() ).get().getClass().getSimpleName()
				.contains( "Jdk" );
	}

	private boolean isRest4() {
		return getDiscoveredClientFactory( configuration.createBeanResolverForTest() ).get().getClass().getSimpleName()
				.contains( "Rest4" );
	}

	private ElasticsearchJsonClientImplementor client(List<String> uris, Map<String, ?> overrides) {
		// These transport fixtures talk exclusively to their local HTTP server, so no cluster discovery/setup is needed.
		Map<String, Object> properties = new HashMap<>();
		properties.put( ElasticsearchBackendSettings.URIS, String.join( ",", uris ) );
		properties.putAll( overrides );
		var resolver = configuration.createBeanResolverForTest();
		return new org.hibernate.search.backend.elasticsearch.client.common.impl.JsonTransportClient(
				getDiscoveredClientFactory( resolver ).get().create( resolver,
						AllAwareConfigurationPropertySource.fromMap( properties ), threads.threadProvider(), "Transport",
						new DelegatingSimpleScheduledExecutor( timeouts, true ) ),
				JsonProviderHelper.create( true ) );
	}

	private static ElasticsearchJsonRequest request(String path) {
		return ElasticsearchJsonRequest.get().pathComponent( URLEncodedString.fromString( path ) ).build();
	}
}
