/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
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
import org.hibernate.search.exception.SearchException;

import org.hibernate.search.elasticsearch.spi.DigestSelfSigningCapable;

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
public final class GsonHttpEntity implements HttpEntity, HttpAsyncContentProducer, DigestSelfSigningCapable {

	private static final byte[] NEWLINE = "\n".getBytes( StandardCharsets.UTF_8 );

	private static final BasicHeader CONTENT_TYPE = new BasicHeader( HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString() );

	/**
	 * N.B. two different buffers of this same size are created.
	 * We want them both of approximately the same "page size",
	 * however one is in characters and the other in bytes.
	 * Considering we hardcoded UTF-8 as encoding, which has an average
	 * conversion ratio of 1.0 this should be close enough.
	 *
	 * It's a rather large size: a tradeoff for very large JSON
	 * documents as we do heavy bulking, and not too large to
	 * be a penalty for small requests.
	 * 1024 has been shown to produce reasonable, TLAB only garbage.
	 */
	private static final int BUFFER_SIZES = 1024;

	private final Gson gson;
	private final List<JsonObject> bodyParts;

	/**
	 * We don't want to compute the length in advance as it would defeat the optimisations
	 * for large bulks.
	 * Still it's possible that we happen to find out, for example if a Digest from all
	 * content needs to be computed, or if the content is small enough as we attempt
	 * to serialise at least one page.
	 */
	private long contentLength;

	/**
	 * We can lazily compute the contentLenght, but we need to avoid changing the value
	 * we report over time as this confuses the Apache HTTP client as it initially defines
	 * the encoding strategy based on this, then assumes it can rely on this being
	 * a constant.
	 * After the {@link #getContentLength()} was invoked at least once, freeze
	 * the value.
	 */
	private boolean contentlengthWasProvided = false;

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
	private ProgressiveCharBufferWriter writer = new ProgressiveCharBufferWriter();

	/**
	 * Add a layer of buffering at the char writer level,
	 * to avoid handling an excessive number of small buffers:
	 */
	private BufferedWriter bufferedWriter = new BufferedWriter( writer, BUFFER_SIZES );

	public GsonHttpEntity(Gson gson, List<JsonObject> bodyParts) {
		Objects.requireNonNull( gson );
		Objects.requireNonNull( bodyParts );
		this.gson = gson;
		this.bodyParts = bodyParts;
		this.contentLength = attemptOnePassEncoding();
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
		this.contentlengthWasProvided = true;
		return this.contentLength;
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
		//Nothing to close but let's make sure we re-wind the stream
		//so that we can start from the beginning if needed
		this.nextBodyToEncodeIndex = 0;
		//Discard previous buffers as they might contain in-process content:
		this.writer = new ProgressiveCharBufferWriter();
		this.bufferedWriter = new BufferedWriter( writer, BUFFER_SIZES );
	}

	/**
	 * Let's see if we can fully encode the content without ever needing to rotate
	 * output buffers. This will allow us to keep the memory consumption reasonable
	 * while also being able to hint the client about the {@link #getContentLength()}.
	 * Incidentally, having this information would avoid chunked output encoding
	 * which is ideal precisely for small messages which can fit into a single buffer.
	 * @return the size of the content, if all was written, or -1.
	 */
	private long attemptOnePassEncoding() {
		// Essentially attempt to use the writer without going NPE on the output sink
		// as it's not set yet.
		try {
			triggerFullWrite();
		}
		catch (IOException e) {
			// Unlikely: there's no output buffer yet!
			throw new SearchException( e );
		}
		if ( writer.flowControlPushingBack == false ) {
			//Can't flip the buffer yet but the position is final,
			//as we know the entire content has been rendered already.
			return writer.availableBuffer.position();
		}
		else {
			return -1l;
		}
	}

	/**
	 * Higher level write loop. It will start writing the JSON objects
	 * from either the  beginning or the next object which wasn't written yet
	 * but simply stop and return as soon as the sink can't accept more data.
	 * Checking state of writer.flowControlPushingBack will reveal if everything
	 * was written.
	 * @throws IOException
	 */
	private void triggerFullWrite() throws IOException {
		while ( nextBodyToEncodeIndex < bodyParts.size() ) {
			JsonObject bodyPart = bodyParts.get( nextBodyToEncodeIndex++ );
			gson.toJson( bodyPart, bufferedWriter );
			bufferedWriter.append( '\n' );
			bufferedWriter.flush();
			if ( writer.flowControlPushingBack ) {
				//Just quit: return control to the caller and trust we'll be called again.
				return;
			}
		}
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

		triggerFullWrite();

		if ( writer.flowControlPushingBack ) {
			//Just quit: return control to the caller and trust we'll be called again.
			return;
		}
		writer.flushToOutput();
		if ( writer.flowControlPushingBack ) {
			//Just quit: return control to the caller and trust we'll be called again.
			return;
		}
		// If we haven't aborted yet, we finished!
		encoder.complete();

		// Design note: we could finally know the content length in bytes at this point
		// (we had an accumulator in previous versions) but that's always pointless
		// as the HTTP CLient will request the size before starting produce content.

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
		hintContentLength( digestWriter.getContentLength() );
	}

	private void hintContentLength(long contentLength) {
		if ( contentlengthWasProvided == false ) {
			this.contentLength = contentLength;
		}
	}

	private static final class ProgressiveCharBufferWriter extends Writer {

		//Initially null: must be set before writing is starter and each
		//time it's resumed as it might change between writes during
		//chunked encoding.
		ContentEncoder out;

		//This is to keep the buffers which could not be written to socket
		//because of flow control. Make sure to eventually push them out.
		//Initialised only if/when needed.
		List<CharBuffer> unwrittenLazyBuffers;

		//Set this to true when we detect clogging, so we can stop trying.
		//Make sure to reset this when the HTTP Client hints so.
		//It's never dangerous to re-enable, just not efficient to try writing
		//unnecessarily.
		boolean flowControlPushingBack = false;

		private final CharsetEncoder utf8Encoder = StandardCharsets.UTF_8.newEncoder();
		private ByteBuffer availableBuffer = ByteBuffer.allocate( BUFFER_SIZES );
		private ByteBuffer needsWritingBuffer;

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {
			CharBuffer toWriteChars = CharBuffer.wrap( cbuf, off, len );
			if ( flowControlPushingBack == false ) {
				writeOrStore( toWriteChars, false, true );
			}
			else {
				storeForLater( toWriteChars );
			}
		}

		public void resumePendingWrites() throws IOException {
			flowControlPushingBack = false;
			if ( availableBuffer == null ) {
				attemptFlushPendingBuffers( false );
			}
		}

		private void storeForLater(CharBuffer toWriteLater) {
			if ( unwrittenLazyBuffers == null ) {
				unwrittenLazyBuffers = new ArrayList<>( 16 );
			}
			//The original CharBuffer is not safe as it's backed by a char array
			//which happens to be the GSON write buffer: it will get mutated.
			CharBuffer defensiveCopy = CharBuffer.allocate( toWriteLater.remaining() );
			defensiveCopy.put( toWriteLater );
			defensiveCopy.flip();
			unwrittenLazyBuffers.add( defensiveCopy );
		}

		private void attemptFlushPendingBuffers(boolean lastWrite) throws IOException {
			flushCurrentBuffer();
			if ( flowControlPushingBack == false && needsWritingBuffer != null ) {
				if ( fullyWrite( needsWritingBuffer ) ) {
					//No longer needed: make it available for reuse by the next writes
					needsWritingBuffer.clear();
					availableBuffer = needsWritingBuffer;
					needsWritingBuffer = null;
				}
			}
			while ( flowControlPushingBack == false && unwrittenLazyBuffers != null && unwrittenLazyBuffers.isEmpty() == false ) {
				CharBuffer charBuffer = unwrittenLazyBuffers.get( 0 );
				if ( writeOrStore( charBuffer, lastWrite, false ) ) {
					unwrittenLazyBuffers.remove( 0 );
				}
			}
		}

		/**
		 * Main writer loop: keeps writing until we either run out of data
		 * to write, or the output buffer signals to back off.
		 * @param toWriteChars the CharBuffer representing the chars to write
		 * @param lastWrite set it to true at the end of the data stream (and never before) to detect malformed streams.
		 * @param saveOnPushback when true an aborted CharBuffer will be enqueued in the write-later buffer. Disable for retries from that same buffer.
		 * @return
		 * @throws IOException
		 */
		private boolean writeOrStore(CharBuffer toWriteChars, boolean lastWrite, boolean saveOnPushback) throws IOException {
			while ( true ) {
				CoderResult coderResult = utf8Encoder.encode( toWriteChars, availableBuffer, lastWrite );
				if ( coderResult.equals( CoderResult.UNDERFLOW ) ) {
					//source consumed. All good, had enough space.
					return true;
				}
				else if ( coderResult.equals( CoderResult.OVERFLOW ) ) {
					//output buffer not large enough. Flush it & repeat:
					flushCurrentBuffer();
					if ( flowControlPushingBack ) {
						//we need to stop pushing data. Save work to continue later & return.
						if ( saveOnPushback ) {
							storeForLater( toWriteChars );
						}
						return false;
					}
					//[else] continue in the while loop to write further data blocks
				}
				else {
					//Encoding exception
					coderResult.throwException();
					return false; //Unreachable
				}
			}
		}

		private void flushCurrentBuffer() throws IOException {
			if ( availableBuffer == null || availableBuffer.position() == 0 ) {
				//Nothing to flush
				return;
			}
			availableBuffer.flip();
			if ( fullyWrite( availableBuffer ) ) {
				availableBuffer.clear();
			}
			else {
				needsWritingBuffer = availableBuffer;
				availableBuffer = null;
			}
		}

		private boolean fullyWrite(ByteBuffer buffer) throws IOException {
			if ( out == null ) {
				flowControlPushingBack = true;
				return false;
			}
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
			// don't flush for real as this is invoked by the
			// wrapping BufferedWriter when we flush that one:
			// we want to control actual flushing independently.
		}

		public void flushToOutput() throws IOException {
			attemptFlushPendingBuffers( true );
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

		public long getContentLength() {
			return this.totalWrittenBytes;
		}

	}

}
