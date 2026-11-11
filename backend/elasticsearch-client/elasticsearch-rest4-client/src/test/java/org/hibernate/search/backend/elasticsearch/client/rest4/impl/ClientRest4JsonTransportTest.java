package org.hibernate.search.backend.elasticsearch.client.rest4.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonHttpEntityContentProvider;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonResponseConsumerScope;
import org.hibernate.search.backend.elasticsearch.json.jackson.JacksonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.testsupport.JsonClientTestSupport;
import org.hibernate.search.util.impl.test.http.StreamingHttpServer;
import org.hibernate.search.util.json.spi.JsonParseException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.apache.http.HttpEntity;
import org.apache.http.HttpVersion;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.client.methods.HttpAsyncMethods;
import org.apache.http.protocol.BasicHttpContext;
import org.elasticsearch.client.RestClient;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class ClientRest4JsonTransportTest {

	@Test
	void malformedGzipStillFailsImmediatelyWhenDecodedLimitIsExceeded() throws Exception {
		var compressed = new ByteArrayOutputStream();
		try ( var gzip = new java.util.zip.GZIPOutputStream( compressed ) ) {
			gzip.write( '<' );
		}
		try ( var gzip = new java.util.zip.GZIPOutputStream( compressed ) ) {
			gzip.write( "remaining error response".getBytes( StandardCharsets.UTF_8 ) );
		}
		var consumer = consumer( 3 );
		var response = response( 503, "application/json" );
		( (BasicHttpEntity) response.getEntity() ).setContentEncoding( "gzip" );
		consumer.responseReceived( response );
		assertThatThrownBy( () -> {
			for ( byte value : compressed.toByteArray() ) {
				consumer.consumeContent( new Decoder( ByteBuffer.wrap( new byte[] { value } ) ), StubIOControl.INSTANCE );
			}
		} ).hasMessageContaining( "byte limit" );
		consumer.close();
	}


	@Test
	@SuppressWarnings("unchecked")
	void decodedLimitIgnoresGzipFramingAndZeroIsUnlimited() throws Exception {
		var compressed = new ByteArrayOutputStream();
		try ( var gzip = new java.util.zip.GZIPOutputStream( compressed ) ) {
			gzip.write( "{}".getBytes( StandardCharsets.UTF_8 ) );
		}
		byte[] bytes = compressed.toByteArray();
		assertThat( bytes.length ).isGreaterThan( 2 );
		for ( int limit : new int[] { 0, 2 } ) {
			var consumer = consumer( limit );
			var response = response( 200, "application/json" );
			var metadata = (BasicHttpEntity) response.getEntity();
			metadata.setContentEncoding( "gzip" );
			metadata.setContentLength( bytes.length );
			consumer.responseReceived( response );
			consumer.consumeContent( new Decoder( ByteBuffer.wrap( bytes ) ), StubIOControl.INSTANCE );
			consumer.responseCompleted( new BasicHttpContext() );
			assertThat( consumer.getException() ).isNull();
			var entity =
					(ClientRest4JsonResponseConsumer.Entity<tools.jackson.databind.JsonNode>) consumer.getResult().getEntity();
			assertThat( entity.body().result().isEmpty() ).isTrue();
			consumer.close();
		}
	}

	@ParameterizedTest
	@ValueSource(longs = { 0, 1, 2 })
	void clientFactoryAppliesConfiguredDecodedResponseLimit(long limit) throws Exception {
		var compressed = new ByteArrayOutputStream();
		try ( var gzip = new java.util.zip.GZIPOutputStream( compressed ) ) {
			gzip.write( "{}".getBytes( StandardCharsets.UTF_8 ) );
		}
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json", "Content-Encoding: gzip\r\n" );
			exchange.chunk( compressed.toByteArray() );
			exchange.end();
		} ) ) {
			var resolver = Mockito.mock( org.hibernate.search.engine.environment.bean.BeanResolver.class );
			Mockito.when( resolver.resolve( List.of() ) ).thenReturn(
					org.hibernate.search.engine.environment.bean.BeanHolder.of( List.of() ) );
			var properties = new org.hibernate.search.engine.cfg.impl.MapConfigurationPropertySource(
					java.util.Map.of( "uris", server.uri(), "response_body_size_limit", limit ) );
			var client = new ClientRest4ElasticsearchClientFactory().create( resolver, properties,
					new org.hibernate.search.engine.environment.thread.impl.EmbeddedThreadProvider( "test" ), "test", null );
			try {
				var request = new org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest(
						"GET", "/", java.util.Map.of(), null, null );
				var result = client.submit( request, JsonClientTestSupport.responseFactory( JacksonJsonHandlers.value() ) );
				if ( limit == 1 ) {
					assertThatThrownBy( () -> result.get( 10, TimeUnit.SECONDS ) ).hasStackTraceContaining( "byte limit" );
				}
				else {
					assertThat( result.get( 10, TimeUnit.SECONDS ).result().isEmpty() ).isTrue();
				}
			}
			finally {
				client.close();
			}
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 8192 })
	void gzipExpansionExceedsDecodedLimit(int fragmentSize) throws Exception {
		byte[] decoded = ( "{\"value\":\"" + "a".repeat( 20000 ) + "\"}" ).getBytes( StandardCharsets.UTF_8 );
		var compressed = new ByteArrayOutputStream();
		try ( var gzip = new GZIPOutputStream( compressed ) ) {
			gzip.write( decoded );
		}
		byte[] bytes = compressed.toByteArray();
		assertThat( bytes.length ).isLessThan( decoded.length - 1 );
		var consumer = consumer( decoded.length - 1 );
		var response = response( 200, "application/json" );
		( (BasicHttpEntity) response.getEntity() ).setContentEncoding( "gzip" );
		consumer.responseReceived( response );
		assertThatThrownBy( () -> {
			for ( int offset = 0; offset < bytes.length; offset += fragmentSize ) {
				consumer.consumeContent( new Decoder( ByteBuffer.wrap( bytes, offset,
						Math.min( fragmentSize, bytes.length - offset ) ) ), StubIOControl.INSTANCE );
			}
		} ).hasMessageContaining( "byte limit" );
		consumer.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	void bodylessStatusesDoNotRequireGzipFraming() throws Exception {
		for ( int status : new int[] { 204, 304 } ) {
			var consumer = consumer( 100 );
			var response = response( status, "application/json" );
			( (BasicHttpEntity) response.getEntity() ).setContentEncoding( "gzip" );
			consumer.responseReceived( response );
			consumer.responseCompleted( new BasicHttpContext() );
			assertThat( consumer.getException() ).isNull();
			var entity =
					(ClientRest4JsonResponseConsumer.Entity<tools.jackson.databind.JsonNode>) consumer.getResult().getEntity();
			assertThat( entity.body().result() ).isNull();
		}
	}

	@Test
	void cancellationBeforeOrBetweenResponseHooksDoesNotEscape() throws Exception {
		for ( boolean beforeHead : List.of( true, false ) ) {
			var scope = new JsonResponseConsumerScope();
			var consumer = consumer( 100, scope );
			var response = response( 200, "application/json" );
			if ( beforeHead ) {
				scope.cancel();
				consumer.responseReceived( response );
			}
			else {
				consumer.onResponseReceived( response );
				scope.cancel();
				consumer.onEntityEnclosed( response.getEntity(), ContentType.APPLICATION_JSON );
			}
			consumer.consumeContent( new Decoder( ByteBuffer.wrap( new byte[] { 123 } ) ), StubIOControl.INSTANCE );
			consumer.responseCompleted( new BasicHttpContext() );
			assertThat( consumer.isDone() ).isTrue();
			assertThat( consumer.getResult() ).isNull();
			assertThat( consumer.getException() ).isNull();
		}
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	@SuppressWarnings("unchecked")
	void responseFragmentationCharsetHeadersGzipAndReplay(int capacity) throws Exception {
		String text = "{\"é😀\":\"" + "é😀".repeat( 5000 ) + "\"}";
		byte[] bytes = text.getBytes( StandardCharsets.UTF_16LE );
		for ( boolean gzip : List.of( false, true ) ) {
			byte[] input = bytes;
			if ( gzip ) {
				ByteArrayOutputStream compressed = new ByteArrayOutputStream();
				try ( GZIPOutputStream stream = new GZIPOutputStream( compressed ) ) {
					stream.write( bytes );
				}
				input = compressed.toByteArray();
			}
			var consumer = consumer(
					Math.max( input.length, bytes.length ) );
			BasicHttpResponse response = response( 200, "application/json; charset=UTF-16LE" );
			response.addHeader( "Warning", "299 test \"warning\"" );
			if ( gzip ) {
				( (BasicHttpEntity) response.getEntity() ).setContentEncoding( "gzip" );
				response.addHeader( "Content-Encoding", "gzip" );
			}
			consumer.responseReceived( response );
			consumer.consumeContent( new Decoder( ByteBuffer.allocate( 0 ) ), StubIOControl.INSTANCE );
			for ( int offset = 0; offset < input.length; offset += capacity ) {
				consumer.consumeContent(
						new Decoder( ByteBuffer.wrap( input, offset, Math.min( capacity, input.length - offset ) ) ),
						StubIOControl.INSTANCE );
			}
			assertThat( consumer.getResult() ).isNull();
			consumer.responseCompleted( new BasicHttpContext() );
			assertThat( consumer.getException() ).isNull();
			var completed = consumer.getResult();
			assertThat( completed.getFirstHeader( "Warning" ).getValue() ).contains( "warning" );
			assertThat( completed.getStatusLine().getReasonPhrase() ).isEqualTo( "fine" );
			if ( gzip ) {
				assertThat( completed.getFirstHeader( "Content-Encoding" ) ).isNull();
			}
			var entity = (ClientRest4JsonResponseConsumer.Entity<tools.jackson.databind.JsonNode>) completed.getEntity();
			assertThat( entity.body().result().asObject().get( "é😀" ).asString() ).isEqualTo( "é😀".repeat( 5000 ) );
			for ( int attempt = 0; attempt < 2; ++attempt ) {
				try ( var stream = entity.getContent() ) {
					assertThat( new String( stream.readAllBytes(), StandardCharsets.UTF_8 ) ).isEqualTo( text );
				}
			}
		}
	}

	@Test
	void completionCanReleaseTheRequestProducerImmediately() throws Exception {
		var entity = new ClientRest4JsonHttpEntity( JsonClientTestSupport.ndjson( JacksonJsonHandlers.object(),
				List.of( JsonNodeFactory.instance.objectNode() ) ) );
		var output = new ByteArrayOutputStream();
		entity.produceContent( new org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.ContentEncoder() {
			@Override
			public int write(ByteBuffer source) {
				int count = source.remaining();
				while ( source.hasRemaining() ) {
					output.write( source.get() );
				}
				return count;
			}

			@Override
			public void complete() {
				entity.close();
			}

			@Override
			public boolean isCompleted() {
				return false;
			}
		} );
		assertThat( output.toString( StandardCharsets.UTF_8 ) ).isEqualTo( "{}\n" );
		try ( var stream = entity.getContent() ) {
			assertThat( stream.readAllBytes() ).isEqualTo( "{}\n".getBytes( StandardCharsets.UTF_8 ) );
		}
	}

	@Test
	void cancellationClosesIndependentClientOwnedRequestStreams() throws Exception {
		ObjectNode document = JsonNodeFactory.instance.objectNode();
		document.put( "value", "é😀".repeat( 10_000 ) );
		var entity = new ClientRest4JsonHttpEntity(
				JsonClientTestSupport.ndjson( JacksonJsonHandlers.object(), List.of( document ) ) );
		try ( var first = entity.getContent(); var second = entity.getContent() ) {
			assertThat( first.read() ).isEqualTo( '{' );
			assertThat( second.read() ).isEqualTo( '{' );
			entity.cancel();
			entity.cancel();
			assertThat( entity.isRepeatable() ).isFalse();
			assertThatThrownBy( first::read ).isInstanceOf( IOException.class );
			assertThatThrownBy( second::read ).isInstanceOf( IOException.class );
			assertThatThrownBy( entity::getContent ).hasMessageContaining( "cancelled" );
			assertThatThrownBy( () -> entity.produceContent( new Sink(), new Sink() ) )
					.hasMessageContaining( "cancelled" );
			var field =
					JsonHttpEntityContentProvider.class
							.getDeclaredField( "cursor" );
			field.setAccessible( true );
			assertThat( field.get( entity ) ).isNull();
		}
	}

	@Test
	void requestCancellationEagerlyReleasesPerAttemptState() throws Exception {
		var scope = new JsonResponseConsumerScope();
		var consumer = consumer( 100, scope );
		consumer.responseReceived( response( 200, "application/json" ) );
		consumer.consumeContent( new Decoder( ByteBuffer.wrap( "{".getBytes( StandardCharsets.UTF_8 ) ) ),
				StubIOControl.INSTANCE );
		var field = ClientRest4JsonResponseConsumer.class.getDeclaredField( "body" );
		field.setAccessible( true );
		var active =
				(org.hibernate.search.util.json.spi.JsonResponseConsumer<?>) field.get( consumer );
		scope.cancel();
		scope.cancel();
		assertThat( field.get( consumer ) ).isNull();
		assertThatThrownBy( active::throwIfFailed ).hasMessageContaining( "cancelled" );
	}

	@Test
	void localHttpParsesBeforeResponseCompletion() throws Exception {
		var parsed = new CountDownLatch( 1 );
		var finish = new CountDownLatch( 1 );
		var observed = new AtomicReference<org.hibernate.search.util.json.spi.JsonResponseConsumer<?>>();
		var bodyField = ClientRest4JsonResponseConsumer.class.getDeclaredField( "body" );
		bodyField.setAccessible( true );
		var consumer = Mockito.spy( consumer( 1000 ) );
		Mockito.doAnswer( invocation -> {
			invocation.callRealMethod();
			observed.set( (org.hibernate.search.util.json.spi.JsonResponseConsumer<?>) bodyField
					.get( consumer ) );
			parsed.countDown();
			return null;
		} ).when( consumer ).consumeContent( ArgumentMatchers.any( ContentDecoder.class ),
				ArgumentMatchers.any( IOControl.class ) );
		try ( var server = new StreamingHttpServer( exchange -> {
			exchange.headers( 200, "application/json", "" );
			exchange.chunk( "<proxy>".getBytes( StandardCharsets.UTF_8 ) );
			assertThat( finish.await( 10, TimeUnit.SECONDS ) ).isTrue();
			exchange.end();
		} ); var client = HttpAsyncClients.createDefault() ) {
			client.start();
			var future = client.execute( HttpAsyncMethods.createGet( server.uri() ),
					consumer, null );
			try {
				assertThat( parsed.await( 10, TimeUnit.SECONDS ) ).isTrue();
				assertThat( future.isDone() ).isFalse();
				assertThatThrownBy( () -> observed.get().throwIfFailed() )
						.isInstanceOf( JsonParseException.class );
			}
			finally {
				finish.countDown();
			}
			assertThat( future.get( 10, TimeUnit.SECONDS ).getStatusLine().getStatusCode() )
					.isEqualTo( 200 );
		}
	}

	@Test
	void requestEncodedStorageStaysBoundedAcrossScalarGrowth() throws Exception {
		var storage =
				JsonHttpEntityContentProvider.class
						.getDeclaredField( "pending" );
		storage.setAccessible( true );
		var cursor =
				JsonHttpEntityContentProvider.class
						.getDeclaredField( "cursor" );
		cursor.setAccessible( true );
		for ( int size : new int[] { 100, 100_000, 1_000_000 } ) {
			ObjectNode document = JsonNodeFactory.instance.objectNode();
			document.put( "value", "x".repeat( size ) );
			var entity = new ClientRest4JsonHttpEntity(
					JsonClientTestSupport.ndjson( JacksonJsonHandlers.object(), List.of( document ) ) );
			Sink sink = new Sink();
			for ( int step = 0; step < 3 && !sink.ended; ++step ) {
				sink.remaining = 1;
				entity.produceContent( sink, sink );
				assertThat( ( (ByteBuffer) storage.get( entity ) ).capacity() ).isEqualTo( 8192 );
				assertThat( sink.output.size() ).isEqualTo( step + 1 );
			}
			entity.close();
			assertThat( cursor.get( entity ) ).isNull();
			assertThat( storage.get( entity ) ).isNull();
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	void malformedResponsesDeferFailureButEnforceLimits() throws Exception {
		for ( String text : List.of( "<proxy>unavailable", "{", "{}{}" ) ) {
			var consumer = consumer( 100 );
			consumer.responseReceived( response( 503, "application/json" ) );
			consumer.consumeContent( new Decoder( ByteBuffer.wrap( text.getBytes( StandardCharsets.UTF_8 ) ) ),
					StubIOControl.INSTANCE );
			consumer.responseCompleted( new BasicHttpContext() );
			assertThat( consumer.getException() ).isNull();
			assertThat( consumer.getResult().getStatusLine().getStatusCode() ).isEqualTo( 503 );
			var entity =
					(ClientRest4JsonResponseConsumer.Entity<tools.jackson.databind.JsonNode>) consumer.getResult().getEntity();
			assertThatThrownBy( () -> entity.body().result() ).isInstanceOf( RuntimeException.class );
			try ( var stream = entity.getContent() ) {
				assertThat( new String( stream.readAllBytes(), StandardCharsets.UTF_8 ) ).isEqualTo( text );
			}
		}
		var consumer = consumer( 3 );
		consumer.responseReceived( response( 503, "application/json" ) );
		consumer.consumeContent( new Decoder( ByteBuffer.wrap( new byte[] { 60 } ) ), StubIOControl.INSTANCE );
		assertThatThrownBy(
				() -> consumer.consumeContent( new Decoder( ByteBuffer.wrap( new byte[3] ) ), StubIOControl.INSTANCE ) )
				.isInstanceOf( RuntimeException.class ).hasMessageContaining( "byte limit" );
		consumer.cancel();
		consumer.close();
	}

	@Test
	@SuppressWarnings("unchecked")
	void emptyAbsentNullAndCancellation() throws Exception {
		for ( String text : List.of( "", " \n\t", "null" ) ) {
			var consumer = consumer( 100 );
			consumer.responseReceived( response( 200, "application/json" ) );
			consumer.consumeContent( new Decoder( ByteBuffer.wrap( text.getBytes( StandardCharsets.UTF_8 ) ) ),
					StubIOControl.INSTANCE );
			consumer.responseCompleted( new BasicHttpContext() );
			var entity =
					(ClientRest4JsonResponseConsumer.Entity<tools.jackson.databind.JsonNode>) consumer.getResult().getEntity();
			assertThat( entity.body().result() )
					.isNull();
		}
		var consumer = consumer( 100 );
		consumer.responseReceived( new BasicHttpResponse( HttpVersion.HTTP_1_1, 204, "fine" ) );
		consumer.responseCompleted( new BasicHttpContext() );
		assertThat( consumer.getResult().getEntity() ).isNull();
		consumer = consumer( 100 );
		consumer.responseReceived( response( 200, "application/json" ) );
		consumer.consumeContent( new Decoder( ByteBuffer.wrap( new byte[] { 123 } ) ), StubIOControl.INSTANCE );
		assertThat( consumer.cancel() ).isTrue();
		consumer.responseCompleted( new BasicHttpContext() );
		assertThat( consumer.getResult() ).isNull();
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 5, 8192 })
	void partialZeroWritesWakeupsAndRetry(int capacity) throws Exception {
		ObjectNode object = JsonNodeFactory.instance.objectNode();
		object.put( "value", "é😀".repeat( 5000 ) );
		var entity = new ClientRest4JsonHttpEntity(
				JsonClientTestSupport.ndjson( JacksonJsonHandlers.object(), List.of( object ) ) );
		Sink sink = new Sink();
		entity.produceContent( sink, sink );
		assertThat( sink.output.size() ).isZero();
		assertThat( sink.wakeups ).isEqualTo( 1 );
		sink.remaining = capacity;
		entity.produceContent( sink, sink );
		entity.close();
		assertThat( sink.output.size() ).isEqualTo( capacity );
		Sink retry = new Sink();
		while ( !retry.ended ) {
			retry.remaining = capacity;
			entity.produceContent( retry, retry );
		}
		assertThat( retry.output.toString( StandardCharsets.UTF_8 ) )
				.isEqualTo( JacksonJsonHandlers.object().toJson( object ) + "\n" );
		assertThat( entity.getContentLength() ).isEqualTo( -1 );
		entity.close();
	}

	@Test
	void pinnedRequestCompressionBuffersAndPropagatesFailure() throws Exception {
		ObjectNode document = JsonNodeFactory.instance.objectNode();
		document.put( "value", "é😀".repeat( 10_000 ) );
		var entity = new ClientRest4JsonHttpEntity(
				JsonClientTestSupport.ndjson( JacksonJsonHandlers.object(), List.of( document ) ) );
		var compressed = new RestClient.ContentCompressingEntity( entity );
		for ( int attempt = 0; attempt < 2; ++attempt ) {
			try ( var input = compressed.getContent() ) {
				// Pinned client wrapper materializes the complete compressed payload before returning this stream.
				assertThat( input ).isInstanceOf( ByteArrayInputStream.class );
				try ( var decoded = new GZIPInputStream( input ) ) {
					assertThat( new String( decoded.readAllBytes(), StandardCharsets.UTF_8 ) )
							.isEqualTo( JacksonJsonHandlers.object().toJson( document ) + "\n" );
				}
			}
		}
		assertThat( entity.getContentLength() ).isEqualTo( -1 );
		var failing = Mockito.mock( HttpEntity.class );
		Mockito.doThrow( new IOException( "source failed" ) ).when( failing )
				.writeTo( ArgumentMatchers.any( OutputStream.class ) );
		var failingCompressed = new RestClient.ContentCompressingEntity( failing );
		assertThatThrownBy( failingCompressed::getContent ).isInstanceOf( IOException.class )
				.hasMessageContaining( "source failed" );
	}

	private static BasicHttpResponse response(int code, String contentType) {
		BasicHttpResponse response = new BasicHttpResponse( HttpVersion.HTTP_1_1, code, "fine" );
		BasicHttpEntity entity = new BasicHttpEntity();
		entity.setContentType( contentType );
		entity.setContentLength( -1 );
		response.setEntity( entity );
		return response;
	}

	private static final class Decoder implements ContentDecoder {
		private final ByteBuffer input;

		Decoder(ByteBuffer input) {
			this.input = input;
		}

		@Override
		public int read(ByteBuffer destination) {
			int count = Math.min( destination.remaining(), input.remaining() );
			for ( int i = 0; i < count; ++i ) {
				destination.put( input.get() );
			}
			return count;
		}

		@Override
		public boolean isCompleted() {
			return !input.hasRemaining();
		}
	}

	private static final class Sink implements ContentEncoder, IOControl {
		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		int remaining;
		int wakeups;
		boolean ended;

		@Override
		public int write(ByteBuffer input) {
			int count = Math.min( remaining, input.remaining() );
			byte[] bytes = new byte[count];
			input.get( bytes );
			output.writeBytes( bytes );
			remaining -= count;
			return count;
		}

		@Override
		public void complete() {
			ended = true;
		}

		@Override
		public boolean isCompleted() {
			return ended;
		}

		@Override
		public void requestOutput() {
			++wakeups;
		}

		@Override
		public void requestInput() {
		}

		@Override
		public void suspendInput() {
		}

		@Override
		public void suspendOutput() {
		}

		@Override
		public void shutdown() {
		}
	}

	private static ClientRest4JsonResponseConsumer<tools.jackson.databind.JsonNode> consumer(long limit) {
		return consumer( limit, new JsonResponseConsumerScope() );
	}

	private static ClientRest4JsonResponseConsumer<tools.jackson.databind.JsonNode> consumer(long limit,
			JsonResponseConsumerScope scope) {
		return new ClientRest4JsonResponseConsumer<>( limit, scope,
				JsonClientTestSupport.responseFactory( JacksonJsonHandlers.value() ) );
	}

}
