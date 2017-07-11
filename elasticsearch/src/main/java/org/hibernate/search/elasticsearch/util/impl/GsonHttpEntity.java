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
import java.security.MessageDigest;
import java.util.ArrayList;
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

import org.hibernate.search.elasticsearch.spi.DigestSelfSigningCapable;
import org.hibernate.search.exception.SearchException;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

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
 * is being produced is not known in advance.
 *
 * @author Sanne Grinovero (C) 2017 Red Hat Inc.
 */
public final class GsonHttpEntity implements HttpEntity, HttpAsyncContentProducer, DigestSelfSigningCapable {

	private static final byte[] NEWLINE = "\n".getBytes( StandardCharsets.UTF_8 );

	private static final BasicHeader CONTENT_TYPE = new BasicHeader( HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString() );

	private final Gson gson;
	private final List<JsonObject> bodyParts;

	/**
	 * We don't want to compute the length in advance as it would defeat all optimisations.
	 * Still it's possible that we happen to find out, for example if a Digest from all
	 * content needs to be computed.
	 */
	private long contentLength = -1l;

	/**
	 * Since flow control might hint to stop producing data,
	 * while we can't interrupt the rendering of a single JSON body
	 * we can avoid starting the rendering of any subsequent JSON body.
	 * So keep track of the next body which still needs to be rendered;
	 * to allow the output to be "repeatable" we also need to reset this
	 * at the end.
	 */
	private int nextBodyToEncodeIndex = 0;

	/**
	 * Adaptor from string output rendered into the actual output sink.
	 * We keep this as a field level attribute as we might have
	 * partially rendered JSON stored in its buffers while flow control
	 * refuses to accept more bytes.
	 */
	private final ProgressiveByteBufWriter writer = new ProgressiveByteBufWriter();

	public GsonHttpEntity(Gson gson, List<JsonObject> bodyParts) {
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
		return contentLength;
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
	public InputStream getContent() throws IOException {
		//This could be implemented but would be sub-optimal compared to using produceContent().
		//We therefore prefer throwing the exception so that we can easily spot unintended usage via tests.
		throw new UnsupportedOperationException( "Not implemented! Expected to produce content only over produceContent()" );
	}

	@Override
	public void writeTo(OutputStream outstream) throws IOException {
		//This could be implemented but would be sub-optimal compared to using produceContent().
		//We therefore prefer throwing the exception so that we can easily spot unintended usage via tests.
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
		// Warning: this method is possibly invoked multiple times, depending on the output buffers
		// to have available space !
		// Production of data is expected to complete only after we invoke ContentEncoder#complete.

		//Re-set the encoder as it might be a different one than a previously used instance:
		writer.out = encoder;

		//First write unfinished business from previous attempts
		writer.resumePendingWrites();
		if ( writer.flowControlPushingBack ) {
			//Just quit: return control to the caller and trust we'll be called again.
			return;
		}

		while ( nextBodyToEncodeIndex < bodyParts.size() ) {
			JsonObject bodyPart = bodyParts.get( nextBodyToEncodeIndex++ );
			gson.toJson( bodyPart, writer );
			writer.insertNewline();
			if ( writer.flowControlPushingBack ) {
				//Just quit: return control to the caller and trust we'll be called again.
				return;
			}
		}

		// If we haven't aborted yet, we finished!
		encoder.complete();
		// We finally know the content length in bytes:
		this.contentLength = writer.getTotalWrittenAndReset();
		//Allow to repeat the content rendering from the beginning:
		this.nextBodyToEncodeIndex = 0;
	}

	@Override
	public void fillDigest(MessageDigest digest) throws IOException {
		//For digest computation we use no pagination, so ignore the mutable fields.
		final DigestWriter digestWriter = new DigestWriter( digest );
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, digestWriter );
			digestWriter.insertNewline();
		}
		//Now we finally know the content size in bytes:
		this.contentLength = digestWriter.getContentLenght();
	}

	private static final class ProgressiveByteBufWriter extends Writer {

		//Initially null: must be set before writing,
		//as it might change between writes.
		ContentEncoder out;

		//This is to keep the buffers which could not be written to socket
		//because of flow control. Make sure to eventually push them out.
		//Initialised only if/when needed.
		List<ByteBuffer> unwrittenLazyBuffers;

		//Set this to true when we detect clogging, so we can stop trying.
		//Make sure to reset this when the HTTP Client hints so.
		//It's never dangerous to re-enable, just not efficient to try writing
		//unnecessarily.
		boolean flowControlPushingBack = false;

		//Accumulate the content length while it's being produced so we can hint
		//other components.
		long totalWrittenBytes = 0;

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			CharBuffer toWriteChars = CharBuffer.wrap( cbuf, off, len );
			ByteBuffer toWriteBytes = StandardCharsets.UTF_8.encode( toWriteChars );
			writeOrBufferWrite( toWriteBytes );
		}

		public long getTotalWrittenAndReset() {
			final long total = totalWrittenBytes;
			//To not count size multiple times on repeated renders:
			totalWrittenBytes = 0;
			return total;
		}

		public void resumePendingWrites() throws IOException {
			flowControlPushingBack = false;
			flushPendingBuffers();
		}

		public void insertNewline() throws IOException {
			writeOrBufferWrite( ByteBuffer.wrap( NEWLINE ) );
		}

		private void writeOrBufferWrite(ByteBuffer toWriteBytes) throws IOException {
			totalWrittenBytes += toWriteBytes.remaining();
			flushPendingBuffers();
			if ( flowControlPushingBack == false ) {
				//If it wasn't all written, buffer for later
				if ( fullyWrite( toWriteBytes ) == false ) {
					addToBuffers( toWriteBytes );
				}
			}
			else {
				addToBuffers( toWriteBytes );
			}
		}

		private void addToBuffers(ByteBuffer toWriteBytes) {
			if ( unwrittenLazyBuffers == null ) {
				unwrittenLazyBuffers = new ArrayList<>( 16 );
			}
			unwrittenLazyBuffers.add( toWriteBytes );
		}

		private void flushPendingBuffers() throws IOException {
			while ( flowControlPushingBack == false && unwrittenLazyBuffers != null && unwrittenLazyBuffers.isEmpty() == false ) {
				ByteBuffer nextBufferToWrite = unwrittenLazyBuffers.get( 0 );
				if ( fullyWrite( nextBufferToWrite ) ) {
					//We can finally discard this one:
					unwrittenLazyBuffers.remove( 0 );
				}
			}
		}

		private boolean fullyWrite(ByteBuffer buffer) throws IOException {
			final int toWrite = buffer.remaining();
			final int actuallyWritten = out.write( buffer );
			if ( toWrite == actuallyWritten ) {
				return true;
			}
			else {
				flowControlPushingBack = true;
				return false;
			}
		}

		@Override
		public void flush() throws IOException {
			// Nothing to do
		}

		@Override
		public void close() throws IOException {
			// Nothing to do
		}

	}

	private static final class DigestWriter extends Writer {

		private final MessageDigest digest;
		private long totalWrittenBytes = 0;

		public DigestWriter(MessageDigest digest) {
			this.digest = digest;
		}

		@Override
		public void write(char[] input, int offset, int len) throws IOException {
			CharBuffer charBuffer = CharBuffer.wrap( input, offset, len );
			ByteBuffer byteBuffer = StandardCharsets.UTF_8.encode( charBuffer );
			this.totalWrittenBytes += byteBuffer.remaining();
			this.digest.update( byteBuffer );
		}

		@Override
		public void flush() throws IOException {
			// Nothing to do
		}

		@Override
		public void close() throws IOException {
			// Nothing to do
		}

		public void insertNewline() {
			this.totalWrittenBytes += NEWLINE.length;
			this.digest.update( NEWLINE );
		}

		public long getContentLenght() {
			return this.totalWrittenBytes;
		}

	}

}
