/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.entity.HttpAsyncContentProducer;

final class HttpAsyncContentProducerInputStream extends InputStream {
	private final HttpAsyncContentProducer contentProducer;
	private final ByteBuffer buffer;
	private final ContentEncoder contentEncoder;

	public HttpAsyncContentProducerInputStream(HttpAsyncContentProducer contentProducer, int bufferSize) {
		this.contentProducer = contentProducer;
		this.buffer = ByteBuffer.allocate( bufferSize );
		this.buffer.limit( 0 );
		this.contentEncoder = new ByteBufferContentEncoder( buffer );
	}

	@Override
	public int read() throws IOException {
		int read = readFromBuffer();
		if ( read < 0 && !contentEncoder.isCompleted() ) {
			writeToBuffer();
			read = readFromBuffer();
		}
		return read;
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		int offset = off;
		int length = len;
		while ( length > 0 && ( buffer.remaining() > 0 || !contentEncoder.isCompleted() ) ) {
			if ( buffer.remaining() == 0 ) {
				writeToBuffer();
			}
			int bytesRead = readFromBuffer( b, offset, length );
			offset += bytesRead;
			length -= bytesRead;
		}
		int totalBytesRead = offset - off;
		if ( totalBytesRead == 0 && contentEncoder.isCompleted() ) {
			return -1;
		}
		return totalBytesRead;
	}

	@Override
	public void close() throws IOException {
		contentProducer.close();
	}

	private void writeToBuffer() throws IOException {
		buffer.clear();
		contentProducer.produceContent( contentEncoder, StubIOControl.INSTANCE );
		buffer.flip();
	}

	private int readFromBuffer() {
		if ( buffer.hasRemaining() ) {
			return buffer.get();
		}
		else {
			return -1;
		}
	}

	private int readFromBuffer(byte[] bytes, int offset, int length) {
		int toRead = Math.min( buffer.remaining(), length );
		if ( toRead > 0 ) {
			buffer.get( bytes, offset, toRead );
		}
		return toRead;
	}

}
