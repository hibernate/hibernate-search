/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.io.IOException;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import org.apache.http.nio.ContentEncoder;

/**
 * A writer to a ContentEncoder, using an automatically growing, paged buffer
 * to store input when flow control pushes back.
 * <p>
 * To be used when your input source is not reactive (uses {@link Writer}),
 * but you have multiple elements to write and thus could take advantage of
 * reactive output to some extent.
 *
 * @author Sanne Grinovero
 */
class ProgressiveCharBufferWriter extends Writer {

	private final CharsetEncoder charsetEncoder;

	/**
	 * Size of buffer pages.
	 */
	private final int pageSize;

	/**
	 * A higher-level buffer for chars, so that we don't have
	 * to wrap every single incoming char[] into a CharBuffer.
	 */
	private final CharBuffer charBuffer;

	/**
	 * Filled buffer pages to be written, in write order.
	 */
	private final Deque<ByteBuffer> needWritingPages = new ArrayDeque<>( 5 );

	/**
	 * Current buffer page, potentially null,
	 * which may have some content but isn't full yet.
	 */
	private ByteBuffer currentPage;

	/**
	 * Initially null: must be set before writing is started and each
	 * time it's resumed as it might change between writes during
	 * chunked encoding.
	 */
	private ContentEncoder output;

	/**
	 * Set this to true when we detect clogging, so we can stop trying.
	 * Make sure to reset this when the HTTP Client hints so.
	 * It's never dangerous to re-enable, just not efficient to try writing
	 * unnecessarily.
	 */
	private boolean flowControlPushingBack = false;

	private int contentLength = 0;

	public ProgressiveCharBufferWriter(Charset charset, int charBufferSize, int pageSize) {
		this.charsetEncoder = charset.newEncoder();
		this.pageSize = pageSize;
		this.charBuffer = CharBuffer.allocate( charBufferSize );
	}

	/**
	 * Set the encoder to write to when buffers are full.
	 */
	public void setOutput(ContentEncoder output) {
		this.output = output;
	}

	// Overrides super.write(int) to remove the synchronized() wrapper.
	// WARNING: when you update this method, make sure to update ALL write(...) methods.
	@Override
	public void write(int c) throws IOException {
		if ( 1 > charBuffer.remaining() ) {
			flush();
		}
		charBuffer.put( (char) c );
	}

	// Overrides super.write(String, int, int) to remove the synchronized() wrapper.
	// WARNING: when you update this method, make sure to update ALL write(...) methods.
	@Override
	public void write(String str, int off, int len) throws IOException {
		// See write(char[], int, int) for comments.
		if ( len > charBuffer.capacity() ) {
			flush();
			writeToByteBuffer( CharBuffer.wrap( str, off, off + len ) );
		}
		else if ( len > charBuffer.remaining() ) {
			flush();
			charBuffer.put( str, off, off + len );
		}
		else {
			charBuffer.put( str, off, off + len );
		}
	}

	// WARNING: when you update this method, make sure to update ALL write(...) methods.
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		if ( len > charBuffer.capacity() ) {
			/*
			 * "cbuf" won't fit in our char buffer, so we'll just write
			 * everything to the byte buffer (first the pending chars in the
			 * char buffer, then "cbuf").
			 */
			flush();
			writeToByteBuffer( CharBuffer.wrap( cbuf, off, len ) );
		}
		else if ( len > charBuffer.remaining() ) {
			/*
			 * We flush the buffer before writing anything in this case.
			 *
			 * If we did not, we'd run the risk of splitting a 3 or 4-byte
			 * character in two parts (one at the end of the buffer before
			 * flushing it, and the other at the beginning after flushing it),
			 * and the encoder would fail when encoding the second part.
			 *
			 * See HSEARCH-2886.
			 */
			flush();
			charBuffer.put( cbuf, off, len );
		}
		else {
			charBuffer.put( cbuf, off, len );
		}
	}

	@Override
	public void flush() throws IOException {
		if ( charBuffer.position() == 0 ) {
			return;
		}
		charBuffer.flip();
		writeToByteBuffer( charBuffer );
		charBuffer.clear();

		// don't flush byte buffers to output as we want to control that flushing independently.
	}

	@Override
	public void close() {
		// Nothing to do
	}

	/**
	 * Send all full buffer pages to the {@link #setOutput(ContentEncoder) output}.
	 * <p>
	 * Flow control may push back, in which case this method or {@link #flushToOutput()}
	 * should be called again later.
	 *
	 * @throws IOException when {@link ContentEncoder#write(ByteBuffer)} fails.
	 */
	public void resumePendingWrites() throws IOException {
		flush();
		flowControlPushingBack = false;
		attemptFlushPendingBuffers( false );
	}

	/**
	 * @return {@code true} if the {@link #setOutput(ContentEncoder) output} pushed
	 * back the last time a write was attempted, {@code false} otherwise.
	 */
	public boolean isFlowControlPushingBack() {
		return flowControlPushingBack;
	}

	/**
	 * Send all buffer pages to the {@link #setOutput(ContentEncoder) output},
	 * Even those that are not full yet
	 * <p>
	 * Flow control may push back, in which case this method should be called again later.
	 *
	 * @throws IOException when {@link ContentEncoder#write(ByteBuffer)} fails.
	 */
	public void flushToOutput() throws IOException {
		flush();
		flowControlPushingBack = false;
		attemptFlushPendingBuffers( true );
	}

	/**
	 * @return The length of the content stored in the byte buffers so far, in bytes.
	 * This does include the content that has already been written to the {@link #setOutput(ContentEncoder) output},
	 * but not the content of the char buffer (which can be flushed to byte buffers using {@link #flush()}).
	 */
	public int contentLength() {
		return contentLength;
	}

	private void writeToByteBuffer(CharBuffer input) throws IOException {
		while ( true ) {
			if ( currentPage == null ) {
				currentPage = ByteBuffer.allocate( pageSize );
			}
			int initialPagePosition = currentPage.position();
			CoderResult coderResult = charsetEncoder.encode( input, currentPage, false );
			contentLength += ( currentPage.position() - initialPagePosition );
			if ( coderResult.equals( CoderResult.UNDERFLOW ) ) {
				return;
			}
			else if ( coderResult.equals( CoderResult.OVERFLOW ) ) {
				// Avoid storing buffers if we can simply flush them
				attemptFlushPendingBuffers( true );
				if ( currentPage != null ) {
					/*
					 * We couldn't flush the current page, but it's full,
					 * so let's move it out of the way.
					 */
					currentPage.flip();
					needWritingPages.add( currentPage );
					currentPage = null;
				}
			}
			else {
				//Encoding exception
				coderResult.throwException();
				return; //Unreachable
			}
		}
	}

	/**
	 * @return {@code true} if this buffer contains content to be written, {@code false} otherwise.
	 */
	private boolean hasRemaining() {
		return !needWritingPages.isEmpty() || currentPage != null && currentPage.position() > 0;
	}

	private void attemptFlushPendingBuffers(boolean flushCurrentPage) throws IOException {
		if ( output == null ) {
			flowControlPushingBack = true;
		}
		if ( flowControlPushingBack || !hasRemaining() ) {
			// Nothing to do
			return;
		}
		Iterator<ByteBuffer> iterator = needWritingPages.iterator();
		while ( iterator.hasNext() && !flowControlPushingBack ) {
			ByteBuffer buffer = iterator.next();
			boolean written = write( buffer );
			if ( written ) {
				iterator.remove();
			}
			else {
				flowControlPushingBack = true;
			}
		}
		if ( flushCurrentPage && !flowControlPushingBack && currentPage != null && currentPage.position() > 0 ) {
			// The encoder still accepts some input, and we are allowed to flush the current page. Let's do.
			currentPage.flip();
			boolean written = write( currentPage );
			if ( !written ) {
				flowControlPushingBack = true;
				needWritingPages.add( currentPage );
			}
			currentPage = null;
		}
	}

	private boolean write(ByteBuffer buffer) throws IOException {
		final int toWrite = buffer.remaining();
		// We should never do 0-length writes, see HSEARCH-2854
		if ( toWrite == 0 ) {
			return true;
		}
		final int actuallyWritten = output.write( buffer );
		return toWrite == actuallyWritten;
	}

}
