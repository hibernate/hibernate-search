/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.lowlevel.reader.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterProvider;
import org.hibernate.search.engine.common.timing.spi.TimingSource;

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
	private final TimingSource timingSource;
	private final int refreshInterval;

	/**
	 * Current open IndexReader, or null when closed.
	 */
	private volatile IndexReaderEntry currentReaderEntry = null;

	public NearRealTimeIndexReaderProvider(IndexWriterProvider indexWriterProvider,
			TimingSource timingSource, int refreshInterval) {
		this.indexWriterProvider = indexWriterProvider;
		this.timingSource = timingSource;
		this.refreshInterval = refreshInterval;
	}

	@Override
	public synchronized void clear() throws IOException {
		setCurrentReaderEntry( null );
	}

	@Override
	public void refresh() {
		IndexReaderEntry entry = currentReaderEntry;

		if ( entry == null ) {
			return;
		}

		entry.forceRefresh();
	}

	@Override
	public DirectoryReader getOrCreate() throws IOException {
		IndexReaderEntry entry = currentReaderEntry;

		// Optimistic locking and checks to avoid synchronization
		if ( entry != null && entry.reader.tryIncRef() ) {
			// Do this *after* tryIncRef,
			// otherwise the reader could get closed between the call to isFresh and the return
			if ( entry.isFresh() ) {
				return entry.reader;
			}
			else {
				entry.reader.decRef();
			}
		}

		return getFreshIndexReader().reader;
	}

	private synchronized IndexReaderEntry getFreshIndexReader() throws IOException {
		IndexReaderEntry oldEntry = currentReaderEntry;
		IndexReaderEntry freshEntry;
		if ( oldEntry == null ) {
			DirectoryReader newReader = indexWriterProvider.getOrCreate().openReader();
			freshEntry = new IndexReaderEntry( newReader, timingSource, refreshInterval );
		}
		else {
			DirectoryReader newReaderOrNull = indexWriterProvider.getOrCreate().openReaderIfChanged( oldEntry.reader );
			if ( newReaderOrNull == null ) {
				// No change, keep the old reader
				freshEntry = oldEntry;
			}
			else {
				freshEntry = new IndexReaderEntry( newReaderOrNull, timingSource, refreshInterval );
			}
		}

		if ( oldEntry != freshEntry ) {
			setCurrentReaderEntry( freshEntry );
		}

		// At this point the reference count is at least one, for the holder.
		// Let's also increment the reference for the caller.
		freshEntry.reader.incRef();

		return freshEntry;
	}

	private synchronized void setCurrentReaderEntry(IndexReaderEntry newEntry) throws IOException {
		IndexReaderEntry oldEntry = currentReaderEntry;
		currentReaderEntry = newEntry;
		if ( oldEntry != null ) {
			// Make sure to close the old reader as soon as no user thread is using it.
			oldEntry.reader.decRef();
		}
	}

	private static class IndexReaderEntry {
		private final DirectoryReader reader;
		private final TimingSource timingSource;
		private final long expiration;

		private volatile boolean refreshForced = false;

		private IndexReaderEntry(DirectoryReader reader, TimingSource timingSource, int refreshInterval) {
			this.reader = reader;
			this.timingSource = timingSource;
			this.expiration = refreshInterval == 0 ? 0 : timingSource.monotonicTimeEstimate() + refreshInterval;
		}

		public void forceRefresh() {
			refreshForced = true;
		}

		/**
		 * @return {@code true} if the reader is still fresh enough to be used,
		 * i.e. if it is completely up-to-date with the state of the index writer
		 * OR is out-of-date by less than the configured refresh interval,
		 * and refresh wasn't forced by a previous write.
		 * @throws IOException If an I/O failure occurs.
		 */
		boolean isFresh() throws IOException {
			if ( refreshForced ) {
				return false;
			}
			if ( expiration == 0 || expiration < timingSource.monotonicTimeEstimate() ) {
				// The last refresh was a long time ago. Let's check if the reader is really fresh.
				return reader.isCurrent();
			}
			else {
				// The last refresh was recent enough. Let's assume the reader is fresh.
				return true;
			}
		}
	}
}
