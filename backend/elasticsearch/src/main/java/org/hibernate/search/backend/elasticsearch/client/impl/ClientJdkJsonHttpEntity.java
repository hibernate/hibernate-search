package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.Flow;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonHttpEntityContentProvider;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonTransportSupport;
import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;


public class ClientJdkJsonHttpEntity extends JsonHttpEntityContentProvider implements HttpRequest.BodyPublisher {

	public static HttpRequest.BodyPublisher toEntity(
			org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest request)
			throws IOException {
		if ( request.body() == null ) {
			return HttpRequest.BodyPublishers.noBody();
		}
		return new ClientJdkJsonHttpEntity( request.body() );
	}

	public ClientJdkJsonHttpEntity(JsonBody body) {
		super( body );
	}


	@Override
	public long contentLength() {
		return getContentLength();
	}

	@Override
	public void subscribe(Flow.Subscriber<? super ByteBuffer> subscriber) {
		Objects.requireNonNull( subscriber );
		Subscription subscription = new Subscription( subscriber );
		try {
			subscriber.onSubscribe( subscription );
		}
		catch (RuntimeException e) {
			subscription.cancel();
			throw e;
		}
	}

	/** One cursor per subscription. Callbacks are serialized, including reentrant demand. */
	private final class Subscription implements Flow.Subscription {
		private final Flow.Subscriber<? super ByteBuffer> subscriber;
		private JsonWriteCursor cursor;
		private long demand;
		private boolean draining;
		private boolean terminated;
		private RuntimeException failure;

		private Subscription(Flow.Subscriber<? super ByteBuffer> subscriber) {
			this.subscriber = subscriber;
		}

		@Override
		public void request(long n) {
			synchronized (this) {
				if ( terminated ) {
					return;
				}
				if ( n <= 0 ) {
					failure = ElasticsearchClientLog.INSTANCE.nonPositiveSubscriptionDemand( n );
				}
				else {
					long sum = demand + n;
					demand = sum < 0 ? Long.MAX_VALUE : sum;
				}
				if ( draining ) {
					return;
				}
				draining = true;
			}
			try {
				while ( true ) {
					ByteBuffer bytes = null;
					boolean complete = false;
					RuntimeException cause;
					synchronized (this) {
						if ( terminated || ( demand == 0 && failure == null ) ) {
							draining = false;
							return;
						}
						cause = failure;
						if ( cause != null ) {
							cancel();
							bytes = null;
							complete = false;
						}
						else {
							try {
								if ( cursor == null ) {
									cursor = openCursor();
								}
								// Recipients can retain storage indefinitely. Never recycle emitted storage.
								bytes = ByteBuffer.allocate( JsonTransportSupport.DEFAULT_BUFFER_SIZE );
								cursor.write( bytes );
								bytes.flip();
								complete = cursor.isComplete();
								if ( bytes.hasRemaining() ) {
									--demand;
								}
							}
							catch (RuntimeException e) {
								cause = e;
								cancel();
							}
						}
					}
					if ( cause != null ) {
						subscriber.onError( cause );
						return;
					}
					if ( bytes.hasRemaining() ) {
						subscriber.onNext( bytes.asReadOnlyBuffer() );
					}
					synchronized (this) {
						complete = complete && !terminated && failure == null;
						if ( complete ) {
							cancel();
						}
					}
					if ( complete ) {
						subscriber.onComplete();
						return;
					}
				}
			}
			catch (RuntimeException e) {
				cancel();
				throw e;
			}
		}

		@Override
		public synchronized void cancel() {
			terminated = true;
			demand = 0;
			if ( cursor != null ) {
				cursor.close();
				cursor = null;
			}
		}
	}

	static boolean isNoBodyPublisher(HttpRequest.BodyPublisher bodyPublisher) {
		return bodyPublisher.contentLength() == 0;
	}
}
