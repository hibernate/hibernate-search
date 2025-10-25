/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.rest5.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.nio.ContentEncoder;
import org.apache.hc.core5.http.nio.DataStreamChannel;


final class ByteBufferDataStreamChannel implements ContentEncoder, DataStreamChannel {
	private final ByteBuffer buffer;
	private boolean complete = false;

	ByteBufferDataStreamChannel(ByteBuffer buffer) {
		this.buffer = buffer;
		if ( !buffer.hasArray() ) {
			throw new IllegalArgumentException( getClass().getName() + " requires a ByteBuffer backed by an array." );
		}
	}

	@Override
	public void requestOutput() {

	}

	@Override
	public int write(ByteBuffer src) {
		int toWrite = Math.min( src.remaining(), buffer.remaining() );
		src.get( buffer.array(), buffer.arrayOffset() + buffer.position(), toWrite );
		buffer.position( buffer.position() + toWrite );
		return toWrite;
	}

	@Override
	public void endStream() throws IOException {
		complete = true;
	}

	@Override
	public void endStream(List<? extends Header> trailers) throws IOException {
		complete = true;
	}

	@Override
	public void complete(List<? extends Header> trailers) throws IOException {
		complete = true;
	}

	@Override
	public boolean isCompleted() {
		return complete;
	}
}
