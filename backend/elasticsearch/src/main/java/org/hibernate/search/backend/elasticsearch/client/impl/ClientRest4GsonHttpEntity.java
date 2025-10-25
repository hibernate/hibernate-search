/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.hibernate.search.backend.elasticsearch.client.common.gson.entity.spi.ContentEncoder;
import org.hibernate.search.backend.elasticsearch.client.common.gson.entity.spi.GsonHttpEntityContentProvider;
import org.hibernate.search.backend.elasticsearch.client.common.spi.ElasticsearchRequest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.protocol.HTTP;

/**
 * Optimised adapter to encode GSON objects into HttpEntity instances.
 * The naive approach was using various StringBuilders; the objects we
 * need to serialise into JSON might get large and this was causing the
 * internal StringBuilder buffers to need frequent resizing and cause
 * problems with excessive allocations.
 *
 * Rather than trying to guess reasonable default sizes for these buffers,
 * we can defer the serialisation to write directly into the ByteBuffer
 * of the HTTP client, this has the additional benefit of making the
 * intermediary buffers short lived.
 *
 * The one complexity to watch for is flow control: when writing into
 * the output buffer chances are that not all bytes are accepted; in
 * this case we have to hold on the remaining portion of data to
 * be written when the flow control is re-enabled.
 *
 * A side effect of this strategy is that the total content length which
 * is being produced is not known in advance. Not reporting the length
 * in advance to the Apache Http client causes it to use chunked-encoding,
 * which is great for large blocks but not optimal for small messages.
 * For this reason we attempt to start encoding into a small buffer
 * upfront: if all data we need to produce fits into that then we can
 * report the content length; if not the encoding completion will be deferred
 * but not resetting so to avoid repeating encoding work.
 *
 * @author Sanne Grinovero (C) 2017 Red Hat Inc.
 */
final class ClientRest4GsonHttpEntity extends GsonHttpEntityContentProvider implements HttpEntity, HttpAsyncContentProducer {

	private static final BasicHeader CONTENT_TYPE =
			new BasicHeader( HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString() );


	public static HttpEntity toEntity(Gson gson, ElasticsearchRequest request) throws IOException {
		final List<JsonObject> bodyParts = request.bodyParts();
		if ( bodyParts.isEmpty() ) {
			return null;
		}
		return new ClientRest4GsonHttpEntity( gson, bodyParts );
	}

	public ClientRest4GsonHttpEntity(Gson gson, List<JsonObject> bodyParts) throws IOException {
		super( gson, bodyParts );
	}

	@Override
	public boolean isRepeatable() {
		return true;
	}

	@Override
	public boolean isChunked() {
		return false;
	}

	@Override
	public Header getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public Header getContentEncoding() {
		//Apparently this is the correct value:
		return null;
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation") // javac warns about this method being deprecated, but we have to implement it
	public void consumeContent() {
		//not used (and deprecated)
	}

	@Override
	public void produceContent(org.apache.http.nio.ContentEncoder encoder, IOControl ioctrl) throws IOException {
		produceContent( toGsonContentEncoder( encoder ) );
	}

	private ContentEncoder toGsonContentEncoder(org.apache.http.nio.ContentEncoder encoder) {
		if ( encoder instanceof ContentEncoder gce ) {
			return gce;
		}
		else {
			return new ClientRest4ContentEncoder( encoder );
		}
	}

	private static class ClientRest4ContentEncoder implements ContentEncoder {

		private final org.apache.http.nio.ContentEncoder encoder;

		public ClientRest4ContentEncoder(org.apache.http.nio.ContentEncoder encoder) {
			this.encoder = encoder;
		}

		@Override
		public int write(ByteBuffer src) throws IOException {
			return encoder.write( src );
		}

		@Override
		public void complete() throws IOException {
			encoder.complete();
		}

		@Override
		public boolean isCompleted() {
			return encoder.isCompleted();
		}
	}
}
