package org.hibernate.search.util.json.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

import org.hibernate.search.util.json.internal.spi.JsonBodies;
import org.hibernate.search.util.json.logging.impl.JsonLog;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;

/** Bounded synchronous adapters over the production cursor. */
public final class JsonBodyOutput {
	private JsonBodyOutput() {
	}

	public static void writeTo(JsonBody source, OutputStream destination) throws IOException {
		Objects.requireNonNull( destination );
		try ( JsonWriteCursor cursor = source.openCursor() ) {
			long length = source.contentLength();
			int capacity = length >= 0
					? (int) Math.min( Math.max( length, 1 ), JsonBodies.DEFAULT_BUFFER_SIZE )
					: JsonBodies.DEFAULT_BUFFER_SIZE;
			ByteBuffer buffer = ByteBuffer.allocate( capacity );
			while ( !cursor.isComplete() ) {
				cursor.write( buffer );
				destination.write( buffer.array(), 0, buffer.position() );
				buffer.clear();
			}
		}
	}

	public static long probeLength(JsonBody source, int maximumBytes) {
		try ( JsonWriteCursor cursor = source.openCursor() ) {
			ByteBuffer buffer = ByteBuffer.allocate( Math.min( maximumBytes, 1024 ) );
			long written = 0;
			while ( written < maximumBytes && !cursor.isComplete() ) {
				buffer.clear();
				buffer.limit( (int) Math.min( buffer.capacity(), maximumBytes - written ) );
				written += cursor.write( buffer );
			}
			return cursor.isComplete() ? written : -1;
		}
	}

	public static InputStream openStream(JsonBody source) {
		return new CursorInputStream( source.openCursor() );
	}

	private static final class CursorInputStream extends InputStream {
		private JsonWriteCursor cursor;
		private ByteBuffer buffer = ByteBuffer.allocate( JsonBodies.DEFAULT_BUFFER_SIZE ).limit( 0 );

		private CursorInputStream(JsonWriteCursor cursor) {
			this.cursor = cursor;
		}

		private boolean availableOutput() throws IOException {
			if ( cursor == null ) {
				throw JsonLog.INSTANCE.bodyStreamClosed();
			}
			if ( !buffer.hasRemaining() && !cursor.isComplete() ) {
				buffer.clear();
				cursor.write( buffer );
				buffer.flip();
			}
			return buffer.hasRemaining();
		}

		@Override
		public int read() throws IOException {
			return availableOutput() ? buffer.get() & 0xff : -1;
		}

		@Override
		public int read(byte[] destination, int offset, int length) throws IOException {
			Objects.checkFromIndexSize( offset, length, destination.length );
			if ( length == 0 ) {
				return 0;
			}
			if ( !availableOutput() ) {
				return -1;
			}
			int count = Math.min( length, buffer.remaining() );
			buffer.get( destination, offset, count );
			return count;
		}

		@Override
		public void close() {
			if ( cursor != null ) {
				cursor.close();
				cursor = null;
				buffer = null;
			}
		}
	}
}
