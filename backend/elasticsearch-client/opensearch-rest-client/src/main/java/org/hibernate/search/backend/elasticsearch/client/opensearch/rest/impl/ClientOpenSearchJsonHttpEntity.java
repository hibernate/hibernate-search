package org.hibernate.search.backend.elasticsearch.client.opensearch.rest.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.ContentEncoder;
import org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi.JsonHttpEntityContentProvider;
import org.hibernate.search.util.json.spi.JsonBody;

import org.apache.hc.core5.function.Supplier;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.nio.AsyncEntityProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;

/**
 * Replayable JSON entity using one bounded pending cursor buffer per asynchronous attempt.
 * Partial/zero transport acceptance retains the unread suffix until output readiness resumes.
 * A bounded independent probe reports small content lengths; large bodies remain chunked.
 * Signing and synchronous reads each open independent cursors. Close/release resets attempt state.
 */
final class ClientOpenSearchJsonHttpEntity extends JsonHttpEntityContentProvider implements HttpEntity, AsyncEntityProducer {

	private static final String CONTENT_TYPE = ContentType.APPLICATION_JSON.toString();

	public static HttpEntity toEntity(org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest request)
			throws IOException {
		if ( request.body() == null ) {
			return null;
		}
		return new ClientOpenSearchJsonHttpEntity( request.body() );
	}

	public ClientOpenSearchJsonHttpEntity(JsonBody body) {
		super( body );
	}


	@Override
	public boolean isRepeatable() {
		return !isCancelled();
	}

	@Override
	public void failed(Exception cause) {
		close();
	}

	@Override
	public boolean isChunked() {
		return false;
	}

	@Override
	public Set<String> getTrailerNames() {
		return Set.of();
	}

	@Override
	public String getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public String getContentEncoding() {
		//Apparently this is the correct value:
		return null;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public Supplier<List<? extends Header>> getTrailers() {
		return null;
	}

	@Override
	public int available() {
		return availableBytes();
	}

	@Override
	public void produce(DataStreamChannel channel) throws IOException {
		try {
			produceContent( new ClientOpenSearchContentEncoder( channel ) );
			if ( availableBytes() > 0 ) {
				channel.requestOutput();
			}
		}
		catch (IOException | RuntimeException e) {
			close();
			throw e;
		}
	}

	@Override
	public void releaseResources() {
		close();
	}

	private static class ClientOpenSearchContentEncoder implements ContentEncoder {

		private final DataStreamChannel channel;

		public ClientOpenSearchContentEncoder(DataStreamChannel channel) {
			this.channel = channel;
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			return channel.write( src );
		}

		@Override
		public void complete() throws IOException {
			channel.endStream();
		}

		@Override
		public boolean isCompleted() {
			return false;
		}
	}
}
