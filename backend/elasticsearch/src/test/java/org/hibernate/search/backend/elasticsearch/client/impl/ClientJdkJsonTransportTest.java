package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService;
import org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonProviderHelper;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.impl.test.http.StreamingHttpServer;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ClientJdkJsonTransportTest {
	private final JsonCodecService json = JsonProviderHelper.create( true ).getCodec();

	@ParameterizedTest
	@ValueSource(ints = { 1, 8192 })
	void gzipExpansionRespectsDecodedLimit(int fragmentSize) throws Exception {
		String text = "{\"value\":\"" + "a".repeat( 20000 ) + "\"}";
		byte[] decoded = text.getBytes( StandardCharsets.UTF_8 );
		var compressed = new ByteArrayOutputStream();
		// A new gzip member must not reset the decoded-byte budget.
		try ( var gzip = new GZIPOutputStream( compressed ) ) {
			gzip.write( decoded, 0, decoded.length / 2 );
		}
		try ( var gzip = new GZIPOutputStream( compressed ) ) {
			gzip.write( decoded, decoded.length / 2, decoded.length - decoded.length / 2 );
		}
		byte[] bytes = compressed.toByteArray();
		assertThat( bytes.length ).isLessThan( decoded.length - 1 );
		for ( int limit : new int[] { decoded.length - 1, decoded.length } ) {
			var subscriber = subscriber( json,
					StandardCharsets.UTF_8, 200, true, limit );
			Demand demand = new Demand();
			subscriber.onSubscribe( demand );
			for ( int offset = 0; offset < bytes.length; offset += fragmentSize ) {
				subscriber.onNext( List.of( ByteBuffer.wrap( bytes, offset,
						Math.min( fragmentSize, bytes.length - offset ) ) ) );
			}
			if ( limit < decoded.length ) {
				assertThat( demand.cancelled ).isTrue();
				assertThatThrownBy( () -> subscriber.getBody().toCompletableFuture().join() )
						.hasStackTraceContaining( "byte limit" );
			}
			else {
				subscriber.onComplete();
				assertThat( subscriber.getBody().toCompletableFuture().join().get( "value" ).asString() )
						.isEqualTo( "a".repeat( 20000 ) );
			}
		}
	}

	@Test
	void serializationFailuresSignalOnErrorAndReleaseTheCursor() throws Exception {
		for ( boolean failOnOpen : List.of( true, false ) ) {
			var failure = new IllegalStateException( "Serialization failed" );
			var cursor = Mockito.mock( JsonWriteCursor.class );
			Mockito.when( cursor.write( ArgumentMatchers.any( ByteBuffer.class ) ) ).thenThrow( failure );
			var publisher = new ClientJdkJsonHttpEntity(
					org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
							.ndjson( List.of( JsonNodeFactory.instance.objectNode() ), json.validation() ) ) {
				@Override
				protected JsonWriteCursor openCursor() {
					if ( failOnOpen ) {
						throw failure;
					}
					return cursor;
				}
			};
			var subscriber = new RecordingSubscriber();
			publisher.subscribe( subscriber );
			subscriber.subscription.request( 1 );
			assertThat( subscriber.failure ).isSameAs( failure );
			assertThat( subscriber.complete ).isFalse();
			assertThat( subscriber.buffers ).isEmpty();
			subscriber.subscription.request( 1 );
			if ( !failOnOpen ) {
				Mockito.verify( cursor ).close();
			}
		}
	}

	@Test
	void subscriberCallbackFailureDoesNotTriggerAnotherCallback() throws Exception {
		var callbackFailure = new IllegalStateException( "Subscriber failed" );
		var publisher = new ClientJdkJsonHttpEntity(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( JsonNodeFactory.instance.objectNode() ), json.validation() ) );
		var subscriber = new RecordingSubscriber() {
			@Override
			public void onNext(ByteBuffer bytes) {
				throw callbackFailure;
			}
		};
		publisher.subscribe( subscriber );
		assertThatThrownBy( () -> subscriber.subscription.request( 1 ) ).isSameAs( callbackFailure );
		assertThat( subscriber.failure ).isNull();
		assertThat( subscriber.complete ).isFalse();
	}

	@Test
	void responseLimitCountsDecodedInputAcrossBatchesAndCancelsImmediately() throws Exception {
		for ( boolean compressed : List.of( false, true ) ) {
			ByteArrayOutputStream output = new ByteArrayOutputStream();
			if ( compressed ) {
				try ( var gzip = new GZIPOutputStream( output ) ) {
					gzip.write( "{}".getBytes( StandardCharsets.UTF_8 ) );
				}
			}
			else {
				output.writeBytes( "{}".getBytes( StandardCharsets.UTF_8 ) );
			}
			byte[] bytes = output.toByteArray();
			for ( int limit : new int[] { 1, 2 } ) {
				var subscriber = subscriber( json,
						StandardCharsets.UTF_8, 200, compressed, limit );
				var demand = new Demand();
				subscriber.onSubscribe( demand );
				subscriber.onNext( List.of( ByteBuffer.wrap( bytes, 0, bytes.length - 1 ) ) );
				subscriber.onNext( List.of( ByteBuffer.wrap( bytes, bytes.length - 1, 1 ) ) );
				if ( limit < 2 ) {
					assertThat( subscriber.getBody().toCompletableFuture() ).isCompletedExceptionally();
					assertThatThrownBy( () -> subscriber.getBody().toCompletableFuture().join() )
							.hasStackTraceContaining( "byte limit" );
					assertThat( demand.cancelled ).isTrue();
				}
				else {
					subscriber.onComplete();
					assertThat( subscriber.getBody().toCompletableFuture().join().isEmpty() ).isTrue();
				}
			}
		}
	}

	@Test
	void bodylessStatusDoesNotRequireGzipFramingButOrdinaryResponseDoes() {
		for ( int status : new int[] { 200, 204, 304 } ) {
			var subscriber = subscriber( json,
					StandardCharsets.UTF_8, status, true );
			subscriber.onSubscribe( new Demand() );
			subscriber.onComplete();
			if ( status == 200 ) {
				assertThatThrownBy( () -> subscriber.getBody().toCompletableFuture().join() )
						.hasStackTraceContaining( "Truncated gzip" );
			}
			else {
				assertThat( subscriber.getBody().toCompletableFuture().join() ).isNull();
			}
		}
	}

	@Test
	@SuppressWarnings("unused")
	void localHttpParsesBeforeResponseCompletion() throws Exception {
		CountDownLatch sent = new CountDownLatch( 1 );
		CountDownLatch finish = new CountDownLatch( 1 );
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json", "" );
			exchange.chunk( "<proxy>".getBytes( StandardCharsets.UTF_8 ) );
			sent.countDown();
			assertThat( finish.await( 10, TimeUnit.SECONDS ) ).isTrue();
			exchange.end();
		} ) ) {
			HttpClient client = HttpClient.newHttpClient();
			var subscriber = subscriber( json,
					StandardCharsets.UTF_8, 200 );
			var request = HttpRequest.newBuilder( URI.create( server.uri() ) ).build();
			var future = client.sendAsync( request, info -> subscriber );
			try {
				assertThat( sent.await( 10, TimeUnit.SECONDS ) ).isTrue();
				assertThatThrownBy( () -> future.get( 10, TimeUnit.SECONDS ) ).hasMessageContaining( "200" );
				assertThat( finish.getCount() ).isEqualTo( 1 );
			}
			finally {
				finish.countDown();
				if ( ( (Object) client ) instanceof AutoCloseable closeable ) {
					closeable.close();
				}
			}
		}
	}

	@Test
	void demandRetainedStorageAndReplay() throws Exception {
		ObjectNode tree = JsonNodeFactory.instance.objectNode();
		tree.put( "text", "é".repeat( 50_000 ) );
		var publisher = new ClientJdkJsonHttpEntity(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( tree, tree ), json.validation() ) );
		assertThat( publisher.contentLength() ).isEqualTo( -1 );
		RecordingSubscriber cancelled = new RecordingSubscriber();
		publisher.subscribe( cancelled );
		cancelled.subscription.cancel();
		cancelled.subscription.request( 1 );
		assertThat( cancelled.buffers ).isEmpty();
		RecordingSubscriber first = new RecordingSubscriber();
		publisher.subscribe( first );
		assertThat( first.buffers ).isEmpty();
		first.subscription.request( 1 );
		assertThat( first.buffers ).hasSize( 1 );
		byte[] retained = bytes( first.buffers.get( 0 ) );
		first.subscription.request( 1 );
		assertThat( first.buffers ).hasSize( 2 );
		assertThat( bytes( first.buffers.get( 0 ) ) ).isEqualTo( retained );
		first.subscription.cancel();
		first.subscription.request( Long.MAX_VALUE );
		assertThat( first.buffers ).hasSize( 2 );
		assertThat( first.complete ).isFalse();
		RecordingSubscriber retry = new RecordingSubscriber();
		publisher.subscribe( retry );
		retry.subscription.request( Long.MAX_VALUE );
		assertThat( retry.complete ).isTrue();
		assertThat( bytes( retry.buffers.get( 0 ) ) ).isEqualTo( retained );
		assertThat( retry.buffers ).allSatisfy( buffer -> assertThat( buffer.remaining() ).isLessThanOrEqualTo( 8192 ) );
		String encoded = collect( retry.buffers );
		assertThat( encoded ).isEqualTo( ( json.write( tree ) + "\n" ).repeat( 2 ) );
		assertThat( publisher.contentLength() ).isEqualTo( -1 );
	}

	@Test
	void reentrantDemandAndInvalidDemand() throws Exception {
		ObjectNode tree = JsonNodeFactory.instance.objectNode();
		tree.put( "value", "x".repeat( 100_000 ) );
		var publisher = new ClientJdkJsonHttpEntity(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( tree ), json.validation() ) );
		RecordingSubscriber subscriber = new RecordingSubscriber() {
			@Override
			public void onNext(ByteBuffer value) {
				super.onNext( value );
				subscription.request( 1 );
			}
		};
		publisher.subscribe( subscriber );
		subscriber.subscription.request( 1 );
		assertThat( subscriber.complete ).isTrue();
		assertThat( collect( subscriber.buffers ) ).isEqualTo( json.write( tree ) + "\n" );
		RecordingSubscriber invalid = new RecordingSubscriber() {
			@Override
			public void onNext(ByteBuffer value) {
				super.onNext( value );
				subscription.request( 0 );
			}
		};
		publisher.subscribe( invalid );
		invalid.subscription.request( Long.MAX_VALUE );
		assertThat( invalid.buffers ).hasSize( 1 );
		assertThat( invalid.failure ).isInstanceOf( IllegalArgumentException.class );
		assertThat( invalid.complete ).isFalse();
		RecordingSubscriber before = new RecordingSubscriber();
		publisher.subscribe( before );
		before.subscription.request( -1 );
		assertThat( before.failure ).isInstanceOf( IllegalArgumentException.class );
		assertThat( before.buffers ).isEmpty();
	}

	@Test
	void concurrentCancellationDuringCallback() throws Exception {
		ObjectNode tree = JsonNodeFactory.instance.objectNode();
		tree.put( "value", "x".repeat( 1_000_000 ) );
		var publisher = new ClientJdkJsonHttpEntity(
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.ElasticsearchJsonBody
						.ndjson( List.of( tree ), json.validation() ) );
		CountDownLatch entered = new CountDownLatch( 1 );
		CountDownLatch release = new CountDownLatch( 1 );
		RecordingSubscriber subscriber = new RecordingSubscriber() {
			@Override
			public void onNext(ByteBuffer value) {
				super.onNext( value );
				entered.countDown();
				try {
					assertThat( release.await( 10, TimeUnit.SECONDS ) ).isTrue();
				}
				catch (InterruptedException e) {
					throw new AssertionFailure( "Interrupted", e );
				}
			}
		};
		publisher.subscribe( subscriber );
		CompletableFuture<Void> drain = CompletableFuture.runAsync( () -> subscriber.subscription.request( Long.MAX_VALUE ) );
		try {
			assertThat( entered.await( 10, TimeUnit.SECONDS ) ).isTrue();
			subscriber.subscription.cancel();
		}
		finally {
			release.countDown();
		}
		drain.get( 10, TimeUnit.SECONDS );
		assertThat( subscriber.buffers ).hasSize( 1 );
		assertThat( subscriber.complete ).isFalse();
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void responseFragmentationAndOwnership(int capacity) {
		for ( int kind = 0; kind < 4; ++kind ) {
			var subscriber = subscriber( json,
					StandardCharsets.UTF_8, 200 );
			Demand demand = new Demand();
			subscriber.onSubscribe( demand );
			byte[] input = ( "{\"é😀\":\"" + "😀é".repeat( 9000 ) + "\"}" ).getBytes( StandardCharsets.UTF_8 );
			subscriber.onNext( List.of( ByteBuffer.allocate( 0 ) ) );
			int batches = 1;
			for ( int offset = 0; offset < input.length; offset += capacity ) {
				int size = Math.min( capacity, input.length - offset );
				ByteBuffer buffer = kind == 1 ? ByteBuffer.allocateDirect( size + 4 ) : ByteBuffer.allocate( size + 4 );
				buffer.position( 2 );
				buffer.put( input, offset, size );
				buffer.flip().position( 2 );
				ByteBuffer supplied = kind == 2 ? buffer.slice() : kind == 3 ? buffer.asReadOnlyBuffer() : buffer;
				subscriber.onNext( List.of( supplied ) );
				assertThat( supplied.hasRemaining() ).isFalse();
				// The parser has consumed the caller's storage before response completion.
				buffer.clear();
				while ( buffer.hasRemaining() ) {
					buffer.put( (byte) 0 );
				}
				++batches;
			}
			assertThat( demand.requested ).isEqualTo( batches + 1 );
			assertThat( subscriber.getBody().toCompletableFuture() ).isNotDone();
			subscriber.onComplete();
			assertThat( subscriber.getBody().toCompletableFuture().join().get( "é😀" ).asString() )
					.isEqualTo( "😀é".repeat( 9000 ) );
		}
	}

	@Test
	void responseErrorsEmptyAndCancellation() {
		for ( String input : List.of( "", " \n\t", "null" ) ) {
			var subscriber = subscriber( json,
					StandardCharsets.UTF_8, 204 );
			subscriber.onSubscribe( new Demand() );
			subscriber.onNext( List.of( ByteBuffer.wrap( input.getBytes( StandardCharsets.UTF_8 ) ) ) );
			subscriber.onComplete();
			assertThat( subscriber.getBody().toCompletableFuture().join() ).isNull();
		}
		for ( String input : List.of( "{", "{} {}", "<html>proxy</html>" ) ) {
			var subscriber = subscriber( json,
					StandardCharsets.UTF_8, 503 );
			Demand demand = new Demand();
			subscriber.onSubscribe( demand );
			subscriber.onNext( List.of( ByteBuffer.wrap( input.getBytes( StandardCharsets.UTF_8 ) ) ) );
			subscriber.onComplete();
			assertThatThrownBy( () -> subscriber.getBody().toCompletableFuture().join() ).hasMessageContaining( "503" );
			assertThat( demand.cancelled ).isTrue();
		}
		var failed =
				subscriber( json, StandardCharsets.UTF_8, 502 );
		Demand failedDemand = new Demand();
		failed.onSubscribe( failedDemand );
		var failure = new IOException( "Interrupted HTTP response" );
		failed.onError( failure );
		assertThatThrownBy( () -> failed.getBody().toCompletableFuture().join() ).hasCause( failure );
		assertThat( failedDemand.cancelled ).isTrue();
		failed.onComplete();
		var cancelledBeforeSubscribe =
				subscriber( json, StandardCharsets.UTF_8, 200 );
		cancelledBeforeSubscribe.getBody().toCompletableFuture().cancel( true );
		Demand lateDemand = new Demand();
		cancelledBeforeSubscribe.onSubscribe( lateDemand );
		assertThat( lateDemand.cancelled ).isTrue();
		var subscriber =
				subscriber( json, StandardCharsets.UTF_8, 200 );
		Demand demand = new Demand();
		subscriber.onSubscribe( demand );
		subscriber.getBody().toCompletableFuture().cancel( true );
		assertThat( demand.cancelled ).isTrue();
		subscriber.onComplete();
		subscriber.onNext( List.of( ByteBuffer.wrap( new byte[] { 123 } ) ) );
		assertThat( subscriber.getBody().toCompletableFuture() ).isCancelled();
	}

	private static byte[] bytes(ByteBuffer buffer) {
		byte[] copy = new byte[buffer.remaining()];
		buffer.duplicate().get( copy );
		return copy;
	}

	private static String collect(List<ByteBuffer> buffers) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		for ( ByteBuffer buffer : buffers ) {
			output.writeBytes( bytes( buffer ) );
		}
		return output.toString( StandardCharsets.UTF_8 );
	}

	private static class RecordingSubscriber implements Flow.Subscriber<ByteBuffer> {
		Flow.Subscription subscription;
		final List<ByteBuffer> buffers = new ArrayList<>();
		boolean complete;
		Throwable failure;

		@Override
		public void onSubscribe(Flow.Subscription subscription) {
			this.subscription = subscription;
		}

		@Override
		public void onNext(ByteBuffer buffer) {
			buffers.add( buffer );
		}

		@Override
		public void onError(Throwable cause) {
			failure = cause;
		}

		@Override
		public void onComplete() {
			complete = true;
		}
	}

	private static class Demand implements Flow.Subscription {
		int requested;
		boolean cancelled;

		@Override
		public void request(long n) {
			assertThat( n ).isEqualTo( 1 );
			++requested;
		}

		@Override
		public void cancel() {
			cancelled = true;
		}
	}

	private static ClientJdkJsonBodySubscriber<tools.jackson.databind.node.ObjectNode> subscriber(
			org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService json,
			java.nio.charset.Charset charset, int status) {
		return subscriber( json, charset, status, false );
	}

	private static ClientJdkJsonBodySubscriber<tools.jackson.databind.node.ObjectNode> subscriber(
			org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService json,
			java.nio.charset.Charset charset, int status, boolean compressed) {
		return subscriber( json, charset, status, compressed,
				org.hibernate.search.backend.elasticsearch.client.common.json.impl.JsonResponseBody.DEFAULT_LIMIT );
	}

	private static ClientJdkJsonBodySubscriber<tools.jackson.databind.node.ObjectNode> subscriber(
			org.hibernate.search.backend.elasticsearch.json.internal.spi.JsonCodecService json,
			java.nio.charset.Charset charset, int status, boolean compressed, long limit) {
		return new ClientJdkJsonBodySubscriber<>( (encoding,
				size) -> new org.hibernate.search.backend.elasticsearch.client.common.json.impl.MappingJsonResponseConsumer<>(
						new org.hibernate.search.backend.elasticsearch.client.common.json.impl.JsonResponseBody(
								encoding, size, json.validation() ),
						tree -> json.decode( tree, tools.jackson.databind.node.ObjectNode.class ) ),
				charset, status, compressed, limit );
	}

}
