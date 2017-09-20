/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.util.impl;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.http.nio.ContentEncoder;

/**
 * An automatically growing, paged buffer.
 * <p>
 * To be used when you need an infinite-size buffer,
 * for example when your class is pushed content to
 * and generate bytes from this content,
 * but cannot always flush them to your consumer.
 *
 * @author Yoann Rodiere
 */
class AutoGrowingPagedBuffer {

	/**
	 * Size of pages allocated by this object.
	 */
	private final int pageSize;

	/**
	 * Filled pages to be written, in write order.
	 */
	private final Deque<ByteBuffer> needWritingPages = new LinkedList<>();

	/**
	 * Current page, potentially null,
	 * which may have some content but isn't full yet.
	 */
	private ByteBuffer currentPage = null;

	public AutoGrowingPagedBuffer(int pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * @return {@code true} if this buffer contains content to be written, {@code false} otherwise.
	 */
	public boolean hasRemaining() {
		return !needWritingPages.isEmpty() || currentPage != null && currentPage.position() > 0;
	}

	/**
	 * @return The number of pages in this buffer currently storing content.
	 */
	public int contentPageCount() {
		return needWritingPages.size() + ( currentPage == null ? 0 : 1 );
	}

	/**
	 * @return The current size of content stored in this buffer, in bytes.
	 * This does not include the content that has already been {@link #writeTo(ContentEncoder) written}.
	 */
	public int contentSize() {
		int pageCount = needWritingPages.size();
		int contentSize = pageCount * pageSize;
		if ( pageCount >= 1 ) {
			ByteBuffer firstBuffer = needWritingPages.getFirst();
			/*
			 * Correct for the size of the first page, which may have been consumed in part
			 * (and may not even be completely full in some cases).
			 */
			contentSize -= pageSize;
			contentSize += firstBuffer.remaining();
		}
		if ( currentPage != null ) {
			// Add the size of the current page, which may not be completely full.
			contentSize += currentPage.position();
		}
		return contentSize;
	}

	/**
	 * Transfer the given characters into this buffer.
	 * @param input The characters to transfer
	 * @param encoder The character encoder
	 * @param endOfInput The {@code enfOfInput} parameter for {@link CharsetEncoder#encode(CharBuffer, ByteBuffer, boolean)}.
	 * @throws CharacterCodingException as thrown by {@link CharsetEncoder#encode(CharBuffer, ByteBuffer, boolean)}
	 */
	public void put(CharBuffer input, CharsetEncoder encoder, boolean endOfInput) throws CharacterCodingException {
		if ( currentPage == null ) {
			currentPage = ByteBuffer.allocate( pageSize );
		}
		while ( true ) {
			CoderResult coderResult = encoder.encode( input, currentPage, endOfInput );
			if ( coderResult.equals( CoderResult.UNDERFLOW ) ) {
				break;
			}
			else if ( coderResult.equals( CoderResult.OVERFLOW ) ) {
				currentPage.flip();
				needWritingPages.add( currentPage );
				currentPage = ByteBuffer.allocate( pageSize );
				continue;
			}
			else {
				//Encoding exception
				coderResult.throwException();
				return; //Unreachable
			}
		}
	}

	/**
	 * @param out The target to flush this buffer to.
	 * @return {@code true} if the buffer could be consumed completely,
	 * {@code false} if flow control pushed back when there was still content to be written.
	 */
	public boolean writeTo(ContentEncoder out) throws IOException {
		Iterator<ByteBuffer> iterator = needWritingPages.iterator();
		boolean flowControlPushingBack = false;
		while ( iterator.hasNext() && !flowControlPushingBack ) {
			ByteBuffer buffer = iterator.next();
			boolean written = writeTo( out, buffer );
			if ( written ) {
				iterator.remove();
			}
			else {
				flowControlPushingBack = true;
			}
		}
		if ( ! flowControlPushingBack && currentPage != null ) {
			// The encoder still accepts some input, let's use the current buffer
			currentPage.flip();
			boolean written = writeTo( out, currentPage );
			if ( !written ) {
				needWritingPages.add( currentPage );
			}
			currentPage = null;
		}
		return !flowControlPushingBack;
	}

	private static boolean writeTo(ContentEncoder out, ByteBuffer buffer) throws IOException {
		final int toWrite = buffer.remaining();
		// We should never do 0-length writes, see HSEARCH-2854
		if ( toWrite == 0 ) {
			return true;
		}
		final int actuallyWritten = out.write( buffer );
		if ( toWrite == actuallyWritten ) {
			return true;
		}
		else {
			return false;
		}
	}

}
