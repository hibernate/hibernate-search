/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.protocol.HTTP;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;

/**
 * Optimised adapter to encode GSON objects into HttpEntity instances.
 * The naive approach was using various StringBuilders and the kind
 * of objects we need to serialize into JSON were causing the internal
 * StringBuilder buffers to need frequent resizing.
 *
 * Rather than trying to guess reasonable default sizes for these buffers,
 * we can defer the serialization to write directly into the ByteBuffer
 * of the HTTP client.
 * By doing so we can use buffer pages of a fixed size, limit it to a
 * reasonable threshold, and reuse it for the next pages.
 *
 * To implement this trick we have to implement both interfaces.
 *
 * @author Sanne Grinovero (C) 2017 Red Hat Inc.
 */
final class GsonHttpEntity implements HttpEntity, HttpAsyncContentProducer {

	/**
	 * A conservative guess at the size of the buffer (in characters!)
	 * needed to hold the produced JSON document.
	 * Picking a too low size will cause CPU overhead, a too large size
	 * will allocate unnecessarily large buffers (waste memory).
	 * The heap consumption is short lived: we aim at fitting in TLAB.
	 */
	private static final int WRITE_BUFFER_SIZE = 512;

	private static final BasicHeader CONTENT_TYPE = new BasicHeader( HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString() );

	private final Gson gson;
	private final List<JsonObject> bodyParts;

	GsonHttpEntity(Gson gson, List<JsonObject> bodyParts) {
		Objects.requireNonNull( gson );
		Objects.requireNonNull( bodyParts );
		this.gson = gson;
		this.bodyParts = bodyParts;
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
	public long getContentLength() {
		//Can't compute the size in advance using this strategy
		return -1;
	}

	@Override
	public Header getContentType() {
		return CONTENT_TYPE;
	}

	@Override
	public Header getContentEncoding() {
		return null;
	}

	@Override
	public InputStream getContent() {
		//This could be implemented but would be inefficient.
		//I therefore prefer throwing the exception so that we can easily spot unintended usage via tests.
		throw new UnsupportedOperationException( "Not implemented! Expected to produce content only over produceContent()" );
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		//This could be implemented but would be inefficient.
		//I therefore prefer throwing the exception so that we can easily spot unintended usage via tests.
		throw new UnsupportedOperationException( "Not implemented! Expected to produce content only over produceContent()" );
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	public void consumeContent() throws IOException {
		//not used (and deprecated)
	}

	@Override
	public void close() throws IOException {
		//Nothing to close
	}

	@Override
	public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
		Objects.requireNonNull( encoder );
		Objects.requireNonNull( ioctrl );

		// We still need to allocate a buffer, but it's going to be of a fixed "page" size.
		// when the page is full rather than growing and copying, we transfer that section
		// into the network buffers.
		// TODO verify if this causes actual network chunking - and if so which sizes we should suggest using.
		BoundedCharBuffer sink = new BoundedCharBuffer( encoder );
		JsonWriter writer = createGsonWriter( sink );

		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, writer );
			sink.appendNewline();
		}
		sink.flush();
		encoder.complete();
	}

	private JsonWriter createGsonWriter(Writer builder) {
		final JsonWriter writer;
		try {
			writer = gson.newJsonWriter( builder );
		}
		catch (IOException e) {
			throw new JsonIOException( e );
		}
		return writer;
	}

	private static final class BoundedCharBuffer extends Writer {

		final CharBuffer buffer = CharBuffer.allocate( WRITE_BUFFER_SIZE );
		final ContentEncoder out;

		BoundedCharBuffer(ContentEncoder underlying) {
			this.out = underlying;
		}

		public void appendNewline() throws IOException {
			if ( buffer.remaining() < 1 ) {
				flush();
			}
			buffer.put( '\n' );
		}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			while ( true ) {
				int toCopy = Math.min( buffer.remaining(), len );
				buffer.put( cbuf, off, toCopy );
				len -= toCopy;
				if ( len > 0 ) { // it didn't fit in the current buffer
					flush();
					off += toCopy;
				}
				else {
					// Done
					return;
				}
			}
		}

		@Override
		public void flush() throws IOException {
			buffer.flip();
			ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode( buffer );
			out.write( byteBuffer );
			buffer.clear();
		}

		@Override
		public void close() {
			//Nothing to close
		}

	}

}
