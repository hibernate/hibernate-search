package org.hibernate.search.backend.elasticsearch.client.common.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonResponseConsumer;
import org.hibernate.search.util.json.spi.JsonResponseConsumerFactory;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

import org.junit.jupiter.api.Test;

/** A transport integration using external contracts, with no JSON library or Search internal imports. */
class ElasticsearchCustomJsonClientTest {
	@Test
	void customTransportPassesDistinctResultTypesAndReplaysBody() {
		ElasticsearchClientFactory factory = (resolver, properties, threads, prefix, timeout) -> new Client();
		try ( var client = factory.create( null, null, null, "custom", null ) ) {
			String json = "{\"value\":\"é😀\",\"number\":9007199254740993}";
			JsonBody body = body( json );
			var request = new ElasticsearchRequest( "POST", "/", Map.of(), body, null );
			var closed = new AtomicBoolean();
			ElasticsearchResponse<String> text = client.submit( request,
					consumerFactory( Function.identity(), closed ) ).join();
			assertThat( text.result() ).isEqualTo( json );
			assertThat( closed ).isTrue();
			closed.set( false );
			ElasticsearchResponse<Integer> length = client.submit( request,
					consumerFactory( String::length, closed ) ).join();
			assertThat( length.result() ).isEqualTo( json.length() );
			assertThat( closed ).isTrue();
		}
	}

	@Test
	void cancellingOneCursorDoesNotAdvanceReplay() throws Exception {
		JsonBody body = body( "{\"value\":1}" );
		try ( var cancelled = body.openCursor() ) {
			cancelled.write( ByteBuffer.allocate( 2 ) );
			cancelled.cancel();
		}
		try ( var replay = body.openStream() ) {
			assertThat( replay.readAllBytes() ).isEqualTo( "{\"value\":1}".getBytes( StandardCharsets.UTF_8 ) );
		}
	}

	private static JsonBody body(String text) {
		byte[] bytes = text.getBytes( StandardCharsets.UTF_8 );
		return new JsonBody() {
			public long contentLength() {
				return bytes.length;
			}

			public JsonWriteCursor openCursor() {
				return new JsonWriteCursor() {
					private ByteBuffer remaining = ByteBuffer.wrap( bytes );

					public int write(ByteBuffer destination) {
						int count = Math.min( remaining.remaining(), destination.remaining() );
						destination.put( remaining.slice( remaining.position(), count ) );
						remaining.position( remaining.position() + count );
						return count;
					}

					public boolean isComplete() {
						return remaining != null && !remaining.hasRemaining();
					}

					public void close() {
						remaining = null;
					}
				};
			}
		};
	}

	private static <T> JsonResponseConsumerFactory<T> consumerFactory(Function<String, T> mapper, AtomicBoolean closed) {
		return (charset, limit) -> new JsonResponseConsumer<>() {
			private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			private RuntimeException failure;
			private boolean complete;

			public void feed(ByteBuffer input) {
				while ( input.hasRemaining() ) {
					bytes.write( input.get() );
				}
			}

			public void complete() {
				complete = true;
			}

			public T result() {
				assertThat( complete ).isTrue();
				throwIfFailed();
				return mapper.apply( bytes.toString( charset ) );
			}

			public void failedParsing(RuntimeException failure) {
				this.failure = failure;
			}

			public void throwIfFailed() {
				if ( failure != null ) {
					throw failure;
				}
			}

			public InputStream openDiagnosticStream() {
				return new ByteArrayInputStream( bytes.toByteArray() );
			}

			public long inputBytes() {
				return bytes.size();
			}

			public void close() {
				closed.set( true );
			}
		};
	}

	private static final class Client implements ElasticsearchClientImplementor {
		public <T> CompletableFuture<ElasticsearchResponse<T>> submit(ElasticsearchRequest request,
				JsonResponseConsumerFactory<T> factory) {
			try ( var consumer = factory.create( StandardCharsets.UTF_8, 1024 );
					var cursor = request.body().openCursor() ) {
				ByteBuffer buffer = ByteBuffer.allocate( 5 );
				while ( !cursor.isComplete() ) {
					cursor.write( buffer );
					buffer.flip();
					consumer.feed( buffer );
					assertThat( buffer.hasRemaining() ).isFalse();
					buffer.clear();
				}
				consumer.complete();
				return CompletableFuture
						.completedFuture( new ElasticsearchResponse<>( "custom", 200, "ok", consumer.result() ) );
			}
			catch (RuntimeException e) {
				return CompletableFuture.failedFuture( e );
			}
		}

		public <T> T unwrap(Class<T> type) {
			return type.cast( this );
		}

		public void close() {
		}
	}
}
