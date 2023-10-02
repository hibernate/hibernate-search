/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.impl.integrationtest.backend.lucene.directory;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.lucene.store.BaseDirectory;
import org.apache.lucene.store.ChecksumIndexInput;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.LockFactory;

class TrackingDirectory extends BaseDirectory {

	private final Directory delegate;
	private final OpenResourceTracker tracker;

	public TrackingDirectory(Directory delegate, LockFactory lockFactory, OpenResourceTracker tracker) {
		super( lockFactory );
		this.delegate = delegate;
		this.tracker = tracker;
		tracker.onOpen( this );
	}

	@Override
	public String[] listAll() throws IOException {
		return delegate.listAll();
	}

	@Override
	public void deleteFile(String name) throws IOException {
		delegate.deleteFile( name );
	}

	@Override
	public long fileLength(String name) throws IOException {
		return delegate.fileLength( name );
	}

	@Override
	public IndexOutput createOutput(String name, IOContext context) throws IOException {
		return new TrackingIndexOutput( delegate.createOutput( name, context ), tracker );
	}

	@Override
	public IndexOutput createTempOutput(String prefix, String suffix, IOContext context) throws IOException {
		return new TrackingIndexOutput( delegate.createTempOutput( prefix, suffix, context ), tracker );
	}

	@Override
	public void sync(Collection<String> names) throws IOException {
		delegate.sync( names );
	}

	@Override
	public void syncMetaData() throws IOException {
		delegate.syncMetaData();
	}

	@Override
	public void rename(String source, String dest) throws IOException {
		delegate.rename( source, dest );
	}

	@Override
	public IndexInput openInput(String name, IOContext context) throws IOException {
		return new TrackingIndexInput( delegate.openInput( name, context ), tracker );
	}

	@Override
	public ChecksumIndexInput openChecksumInput(String name, IOContext context) throws IOException {
		return new TrackingChecksumIndexInput( delegate.openChecksumInput( name, context ), tracker );
	}

	@Override
	public void close() throws IOException {
		delegate.close();
		tracker.onClose( this );
	}

	@Override
	public void copyFrom(Directory from, String src, String dest, IOContext context) throws IOException {
		delegate.copyFrom( from, src, dest, context );
	}

	@Override
	public String toString() {
		return delegate.toString();
	}

	@Override
	public Set<String> getPendingDeletions() throws IOException {
		return delegate.getPendingDeletions();
	}

}
