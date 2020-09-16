/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
		src.get( buffer.array(), buffer.arrayOffset(), toWrite );
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
