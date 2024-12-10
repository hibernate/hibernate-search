/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.directory;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.IndexOutput;

class TrackingIndexOutput extends IndexOutput {

	private final IndexOutput delegate;
	private final OpenResourceTracker tracker;

	public TrackingIndexOutput(IndexOutput delegate, OpenResourceTracker tracker) {
		super( "TrackingIndexOutput(" + delegate.toString() + ")", delegate.getName() );
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
	public long getFilePointer() {
		return delegate.getFilePointer();
	}

	@Override
	public long getChecksum() throws IOException {
		return delegate.getChecksum();
	}

	@Override
	public void writeByte(byte b) throws IOException {
		delegate.writeByte( b );
	}

	@Override
	public void writeBytes(byte[] b, int length) throws IOException {
		delegate.writeBytes( b, length );
	}

	@Override
	public void writeBytes(byte[] b, int offset, int length) throws IOException {
		delegate.writeBytes( b, offset, length );
	}

	@Override
	public void writeInt(int i) throws IOException {
		delegate.writeInt( i );
	}

	@Override
	public void writeShort(short i) throws IOException {
		delegate.writeShort( i );
	}

	@Override
	public void writeLong(long i) throws IOException {
		delegate.writeLong( i );
	}

	@Override
	public void writeString(String s) throws IOException {
		delegate.writeString( s );
	}

	@Override
	public void copyBytes(DataInput input, long numBytes) throws IOException {
		delegate.copyBytes( input, numBytes );
	}

	@Override
	public void writeMapOfStrings(Map<String, String> map) throws IOException {
		delegate.writeMapOfStrings( map );
	}

	@Override
	public void writeSetOfStrings(Set<String> set) throws IOException {
		delegate.writeSetOfStrings( set );
	}
}
