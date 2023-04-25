/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.directory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.RandomAccessInput;

class TrackingIndexInput extends IndexInput {

	private final IndexInput delegate;
	private final OpenResourceTracker tracker;

	public TrackingIndexInput(IndexInput delegate, OpenResourceTracker tracker) {
		super( "TrackingIndexInput(" + delegate.toString() + ")" );
		this.delegate = delegate;
		this.tracker = tracker;
		tracker.onOpen( this );
	}

	@Override
	public final void close() throws IOException {
		delegate.close();
		tracker.onClose( this );
	}

	@Override
	public IndexInput clone() {
		// Lucene never closes clones, so we don't track those.
		return delegate.clone();
	}

	@Override
	public long getFilePointer() {
		return delegate.getFilePointer();
	}

	@Override
	public void seek(long pos) throws IOException {
		delegate.seek( pos );
	}

	@Override
	public long length() {
		return delegate.length();
	}

	@Override
	public IndexInput slice(String sliceDescription, long offset, long length) throws IOException {
		return delegate.slice( sliceDescription, offset, length );
	}

	@Override
	public RandomAccessInput randomAccessSlice(long offset, long length) throws IOException {
		return delegate.randomAccessSlice( offset, length );
	}

	@Override
	public byte readByte() throws IOException {
		return delegate.readByte();
	}

	@Override
	public void readBytes(byte[] b, int offset, int len) throws IOException {
		delegate.readBytes( b, offset, len );
	}

	@Override
	public void readBytes(byte[] b, int offset, int len, boolean useBuffer) throws IOException {
		delegate.readBytes( b, offset, len, useBuffer );
	}

	@Override
	public short readShort() throws IOException {
		return delegate.readShort();
	}

	@Override
	public int readInt() throws IOException {
		return delegate.readInt();
	}

	@Override
	public int readVInt() throws IOException {
		return delegate.readVInt();
	}

	@Override
	public int readZInt() throws IOException {
		return delegate.readZInt();
	}

	@Override
	public long readLong() throws IOException {
		return delegate.readLong();
	}

	@Override
	public long readVLong() throws IOException {
		return delegate.readVLong();
	}

	@Override
	public long readZLong() throws IOException {
		return delegate.readZLong();
	}

	@Override
	public String readString() throws IOException {
		return delegate.readString();
	}

	@Override
	public Map<String, String> readMapOfStrings() throws IOException {
		return delegate.readMapOfStrings();
	}

	@Override
	public Set<String> readSetOfStrings() throws IOException {
		return delegate.readSetOfStrings();
	}

	@Override
	public void skipBytes(long numBytes) throws IOException {
		delegate.skipBytes( numBytes );
	}
}
