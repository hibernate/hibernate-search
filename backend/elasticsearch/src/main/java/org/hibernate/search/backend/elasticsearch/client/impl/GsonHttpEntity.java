/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.hibernate.search.util.common.impl.Contracts;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.entity.HttpAsyncContentProducer;
import org.apache.http.protocol.HTTP;

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
final class GsonHttpEntity implements HttpEntity, HttpAsyncContentProducer {

	private static final Charset CHARSET = StandardCharsets.UTF_8;

	private static final BasicHeader CONTENT_TYPE =
			new BasicHeader( HTTP.CONTENT_TYPE, ContentType.APPLICATION_JSON.toString() );

	/**
	 * The size of byte buffer pages in {@link ProgressiveCharBufferWriter}
	 * It's a rather large size: a tradeoff for very large JSON
	 * documents as we do heavy bulking, and not too large to
	 * be a penalty for small requests.
	 * 1024 has been shown to produce reasonable, TLAB only garbage.
	 */
	private static final int BYTE_BUFFER_PAGE_SIZE = 1024;

	/**
	 * We want the char buffer and byte buffer pages of approximately
	 * the same size, however one is in characters and the other in bytes.
	 * Considering we hardcoded UTF-8 as encoding, which has an average
	 * conversion ratio of almost 1.0, this should be close enough.
	 */
	private static final int CHAR_BUFFER_SIZE = BYTE_BUFFER_PAGE_SIZE;

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
	 * We can lazily compute the contentLength, but we need to avoid changing the value
	 * we report over time as this confuses the Apache HTTP client as it initially defines
	 * the encoding strategy based on this, then assumes it can rely on this being
	 * a constant.
	 * After the {@link #getContentLength()} was invoked at least once, freeze
	 * the value.
	 */
	private boolean contentLengthWasProvided = false;

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
	private ProgressiveCharBufferWriter writer =
			new ProgressiveCharBufferWriter( CHARSET, CHAR_BUFFER_SIZE, BYTE_BUFFER_PAGE_SIZE );

	public GsonHttpEntity(Gson gson, List<JsonObject> bodyParts) throws IOException {
		Contracts.assertNotNull( gson, "gson" );
		Contracts.assertNotNull( bodyParts, "bodyParts" );
		this.gson = gson;
		this.bodyParts = bodyParts;
		this.contentLength = -1;
		attemptOnePassEncoding();
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
		this.contentLengthWasProvided = true;
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
	public InputStream getContent() {
		return new HttpAsyncContentProducerInputStream( this, BYTE_BUFFER_PAGE_SIZE );
	}

	@Override
	public void writeTo(OutputStream out) throws IOException {
		/*
		 * For this method we use no pagination, so ignore the mutable fields.
		 *
		 * Note we don't close the counting stream or the writer,
		 * because we must not close the output stream that was passed as a parameter.
		 */
		CountingOutputStream countingStream = new CountingOutputStream( out );
		Writer outWriter = new OutputStreamWriter( countingStream, CHARSET );
		for ( JsonObject bodyPart : bodyParts ) {
			gson.toJson( bodyPart, outWriter );
			outWriter.append( '\n' );
		}
		outWriter.flush();
		//Now we finally know the content size in bytes:
		hintContentLength( countingStream.getBytesWritten() );
	}

	@Override
	public boolean isStreaming() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation") // javac warns about this method being deprecated, but we have to implement it
	public void consumeContent() {
		//not used (and deprecated)
	}

	@Override
	public void close() {
		//Nothing to close but let's make sure we re-wind the stream
		//so that we can start from the beginning if needed
		this.nextBodyToEncodeIndex = 0;
		//Discard previous buffers as they might contain in-process content:
		this.writer = new ProgressiveCharBufferWriter( CHARSET, CHAR_BUFFER_SIZE, BYTE_BUFFER_PAGE_SIZE );
	}

	/**
	 * Let's see if we can fully encode the content with a minimal write,
	 * i.e. only one body part.
	 * This will allow us to keep the memory consumption reasonable
	 * while also being able to hint the client about the {@link #getContentLength()}.
	 * Incidentally, having this information would avoid chunked output encoding
	 * which is ideal precisely for small messages which can fit into a single buffer.
	 *
	 * @throws IOException This is unlikely to be caused by a real IO operation as there's no output buffer yet,
	 * but it could also be triggered by the UTF8 encoding operations.
	 */
	private void attemptOnePassEncoding() throws IOException {
		// Essentially attempt to use the writer without going NPE on the output sink
		// as it's not set yet.
		triggerFullWrite();
		if ( nextBodyToEncodeIndex == bodyParts.size() ) {
			writer.flush();
			// The buffer's content length so far is the final content length,
			// as we know the entire content has been encoded already.
			hintContentLength( writer.contentLength() );
		}
	}

	/**
	 * Higher level write loop. It will start writing the JSON objects
	 * from either the  beginning or the next object which wasn't written yet
	 * but simply stop and return as soon as the sink can't accept more data.
	 * Checking state of writer.flowControlPushingBack will reveal if everything
	 * was written.
	 * @throws IOException If writing fails.
	 */
	private void triggerFullWrite() throws IOException {
		while ( nextBodyToEncodeIndex < bodyParts.size() ) {
			JsonObject bodyPart = bodyParts.get( nextBodyToEncodeIndex++ );
			gson.toJson( bodyPart, writer );
			writer.append( '\n' );
			writer.flush();
			if ( writer.isFlowControlPushingBack() ) {
				//Just quit: return control to the caller and trust we'll be called again.
				return;
			}
		}
	}

	@Override
	public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
		Contracts.assertNotNull( encoder, "encoder" );
		// Warning: this method is possibly invoked multiple times, depending on the output buffers
		// to have available space !
		// Production of data is expected to complete only after we invoke ContentEncoder#complete.

		//Re-set the encoder as it might be a different one than a previously used instance:
		writer.setOutput( encoder );

		//First write unfinished business from previous attempts
		writer.resumePendingWrites();
		if ( writer.isFlowControlPushingBack() ) {
			//Just quit: return control to the caller and trust we'll be called again.
			return;
		}

		triggerFullWrite();

		if ( writer.isFlowControlPushingBack() ) {
			//Just quit: return control to the caller and trust we'll be called again.
			return;
		}
		writer.flushToOutput();
		if ( writer.isFlowControlPushingBack() ) {
			//Just quit: return control to the caller and trust we'll be called again.
			return;
		}

		// If we haven't aborted yet, we finished!

		// The buffer's content length so far is the final content length,
		// as we know the entire content has been encoded already.
		// Hint at the content length.
		// Note this is only useful if produceContent was called by some process
		// that is not the HTTP client itself (e.g. for request signing),
		// because the HTTP Client itself will request the size before it starts writing content.
		hintContentLength( writer.contentLength() );

		encoder.complete();
	}

	private void hintContentLength(long contentLength) {
		if ( !contentLengthWasProvided ) {
			this.contentLength = contentLength;
		}
	}

}
