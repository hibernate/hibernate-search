/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.nio.ByteBuffer;

import org.apache.http.nio.ContentEncoder;

final class ByteBufferContentEncoder implements ContentEncoder {
	private final ByteBuffer buffer;
	private boolean complete = false;

	ByteBufferContentEncoder(ByteBuffer buffer) {
		this.buffer = buffer;
		if ( !buffer.hasArray() ) {
			throw new IllegalArgumentException( getClass().getName() + " requires a ByteBuffer backed by an array." );
		}
	}

	@Override
	public int write(ByteBuffer src) {
		int toWrite = Math.min( src.remaining(), buffer.remaining() );
		src.get( buffer.array(), buffer.arrayOffset() + buffer.position(), toWrite );
		buffer.position( buffer.position() + toWrite );
		return toWrite;
	}

	@Override
	public void complete() {
		complete = true;
	}

	@Override
	public boolean isCompleted() {
		return complete;
	}
}
