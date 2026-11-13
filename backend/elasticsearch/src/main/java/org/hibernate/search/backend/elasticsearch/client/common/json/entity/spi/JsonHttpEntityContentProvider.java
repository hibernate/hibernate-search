package org.hibernate.search.backend.elasticsearch.client.common.json.entity.spi;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.spi.ElasticsearchClientLog;
import org.hibernate.search.util.common.annotation.Incubating;
import org.hibernate.search.util.json.spi.JsonBody;
import org.hibernate.search.util.json.spi.JsonWriteCursor;


/** Bounded cursor adapter preserving the existing synchronous entity lifecycle. */
@Incubating
public class JsonHttpEntityContentProvider implements ContentProducer {
	private final JsonBody body;
	private JsonWriteCursor cursor;
	private ByteBuffer pending;
	private boolean outputComplete;
	private volatile boolean cancelled;
	private final Set<InputStream> streams = new HashSet<>();


	public JsonHttpEntityContentProvider(JsonBody body) {
		this.body = Objects.requireNonNull( body );
	}

	public long getContentLength() {
		return body.contentLength();
	}

	public synchronized InputStream getContent() {
		if ( cancelled ) {
			throw ElasticsearchClientLog.INSTANCE.requestCancelled();
		}
		InputStream stream = new FilterInputStream( body.openStream() ) {
			@Override
			public synchronized int read() throws IOException {
				return super.read();
			}

			@Override
			public synchronized int read(byte[] target, int offset, int length) throws IOException {
				return super.read( target, offset, length );
			}

			@Override
			public synchronized void close() throws IOException {
				try {
					super.close();
				}
				finally {
					removeStream( this );
				}
			}
		};
		streams.add( stream );
		return stream;
	}

	private synchronized void removeStream(InputStream stream) {
		streams.remove( stream );
	}

	protected JsonWriteCursor openCursor() {
		return body.openCursor();
	}

	public void writeTo(OutputStream out) throws IOException {
		try ( InputStream stream = getContent() ) {
			stream.transferTo( out );
		}
	}

	public synchronized void close() {
		if ( cursor != null ) {
			cursor.close();
			cursor = null;
		}
		pending = null;
		outputComplete = false;
	}

	/** Permanently cancels this request, including streams owned by a client's entity producer. */
	public void cancel() {
		cancelled = true;
		List<InputStream> active;
		synchronized (this) {
			close();
			active = List.copyOf( streams );
			streams.clear();
		}
		for ( InputStream stream : active ) {
			try {
				stream.close();
			}
			catch (IOException e) {
				// Cursor-backed streams have no external resource whose close can fail.
			}
		}
	}

	protected boolean isCancelled() {
		return cancelled;
	}

	protected synchronized int availableBytes() {
		return cancelled || outputComplete ? 0 : JsonTransportSupport.DEFAULT_BUFFER_SIZE;
	}

	public synchronized void produceContent(ContentEncoder encoder) throws IOException {
		if ( cancelled ) {
			throw ElasticsearchClientLog.INSTANCE.requestOutputCancelled();
		}
		if ( outputComplete ) {
			encoder.complete();
			return;
		}
		if ( cursor == null ) {
			cursor = body.openCursor();
			pending = ByteBuffer.allocate( JsonTransportSupport.DEFAULT_BUFFER_SIZE );
			pending.limit( 0 );
		}
		while ( true ) {
			if ( cancelled ) {
				throw ElasticsearchClientLog.INSTANCE.requestOutputCancelled();
			}
			if ( pending.hasRemaining() ) {
				int accepted = encoder.write( pending.slice() );
				if ( accepted < 0 || accepted > pending.remaining() ) {
					throw ElasticsearchClientLog.INSTANCE.invalidEncodedByteCount( accepted );
				}
				pending.position( pending.position() + accepted );
				if ( pending.hasRemaining() ) {
					return;
				}
			}
			if ( cursor.isComplete() ) {
				cursor.close();
				cursor = null;
				pending = null;
				outputComplete = true;
				encoder.complete();
				return;
			}
			pending.clear();
			cursor.write( pending );
			pending.flip();
		}
	}
}
