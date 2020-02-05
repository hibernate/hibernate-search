/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;

import org.apache.lucene.index.DirectoryReader;

/**
 * A index reader holder that opens an index reader from the index writer,
 * thereby providing a near-real-time view on the index.
 * <p>
 * The index reader is instantiated once and shared among all threads
 * for as long as it is deemed "current",
 * i.e. as long as there were no changes
 * from the directory every time an index reader is requested.
 * <p>
 * Heavily inspired by {@code org.hibernate.search.backend.impl.lucene.NRTWorkspaceImpl} from Hibernate Search 5
 * by Sanne Grinovero.
 */
public class NearRealTimeIndexReaderProvider implements IndexReaderProvider {

	private final IndexWriterProvider indexWriterProvider;

	/**
	 * Current open IndexReader, or null when closed.
	 */
	private volatile DirectoryReader currentReader = null;

	public NearRealTimeIndexReaderProvider(IndexWriterProvider indexWriterProvider) {
		this.indexWriterProvider = indexWriterProvider;
	}

	@Override
	public synchronized void clear() throws IOException {
		setCurrentReader( null );
	}

	@Override
	public DirectoryReader getOrCreate() throws IOException {
		DirectoryReader indexReader = currentReader;

		// Optimistic checks and locking to avoid synchronization
		if ( indexReader != null && indexReader.isCurrent() && indexReader.tryIncRef() ) {
			return indexReader;
		}

		return getFreshIndexReader();
	}

	private synchronized DirectoryReader getFreshIndexReader() throws IOException {
		DirectoryReader oldReader = currentReader;
		DirectoryReader freshReader;
		if ( oldReader == null ) {
			freshReader = indexWriterProvider.getOrCreate().openReader();
		}
		else {
			freshReader = indexWriterProvider.getOrCreate().openReaderIfChanged( oldReader );
			if ( freshReader == null ) {
				// No change, keep the old reader
				freshReader = oldReader;
			}
		}

		if ( oldReader != freshReader ) {
			setCurrentReader( freshReader );
		}

		// At this point the reference count is at least one, for the holder.
		// Let's also increment the reference for the caller.
		freshReader.incRef();

		return freshReader;
	}

	private synchronized void setCurrentReader(DirectoryReader newReader) throws IOException {
		DirectoryReader oldReader = currentReader;
		currentReader = newReader;
		if ( oldReader != null ) {
			// Make sure to close the old reader as soon as no user thread is using it.
			oldReader.decRef();
		}
	}
}
