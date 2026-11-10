package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public final class HttpAsyncContentProducerInputStream extends InputStream {
	private final ContentProducer contentProducer;
	private final ByteBuffer buffer;
	private final ContentEncoder contentEncoder;

	public HttpAsyncContentProducerInputStream(ContentProducer contentProducer, int bufferSize) {
		if ( bufferSize <= 0 ) {
			throw new IllegalArgumentException( "Buffer size must be positive" );
		}
		this.contentProducer = Objects.requireNonNull( contentProducer );
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
		Objects.checkFromIndexSize( off, len, b.length );
		if ( len == 0 ) {
			return 0;
		}
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
		contentProducer.produceContent( contentEncoder );
		buffer.flip();
		if ( !buffer.hasRemaining() && !contentEncoder.isCompleted() ) {
			throw new IOException( "Content producer made no progress" );
		}
	}

	private int readFromBuffer() {
		if ( buffer.hasRemaining() ) {
			return Byte.toUnsignedInt( buffer.get() );
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
