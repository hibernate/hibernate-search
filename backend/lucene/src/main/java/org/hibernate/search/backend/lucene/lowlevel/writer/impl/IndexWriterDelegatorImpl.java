/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexWriterDelegatorImpl implements IndexWriterDelegator {

	private final IndexWriter delegate;
	private final TimingSource timingSource;
	private final int commitInterval;
	private final Object commitOrDelayLock = new Object();

	private long commitExpiration;

	public IndexWriterDelegatorImpl(IndexWriter delegate, TimingSource timingSource, int commitInterval) {
		this.delegate = delegate;
		this.timingSource = timingSource;
		this.commitInterval = commitInterval;
		updateCommitExpiration();
	}

	@Override
	public long addDocuments(Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException {
		return delegate.addDocuments( docs );
	}

	@Override
	public long updateDocuments(Term term, Iterable<? extends Iterable<? extends IndexableField>> docs) throws IOException {
		return delegate.updateDocuments( term, docs );
	}

	@Override
	public long deleteDocuments(Term term) throws IOException {
		return delegate.deleteDocuments( term );
	}

	@Override
	public long deleteDocuments(Query query) throws IOException {
		return delegate.deleteDocuments( query );
	}

	public void mergeSegments() throws IOException {
		delegate.forceMerge( 1 );
	}

	public void commit() throws IOException {
		doCommit();
	}

	public long commitOrDelay() throws IOException {
		if ( !delegate.hasUncommittedChanges() ) {
			// No need to either commit or plan a delayed commit: there's nothing to commit.
			return 0L;
		}

		long timeToCommit = getTimeToCommit();
		if ( timeToCommit > 0L ) {
			return timeToCommit;
		}

		// Synchronize in order to prevent a scenario where two threads call commitOrDelay() concurrently,
		// and both notice the last commit has expired,
		// resulting in two commits where one would have been enough.
		// Concurrent commits may still happen if a thread calls commit() concurrently:
		// this is fine and actually desired behavior.
		// We only care about concurrent calls to commitOrDelay() here.
		synchronized (commitOrDelayLock) {
			timeToCommit = getTimeToCommit();
			if ( timeToCommit > 0L ) {
				return timeToCommit;
			}
			doCommit();
			return 0L;
		}
	}

	public DirectoryReader openReader() throws IOException {
		return DirectoryReader.open( delegate );
	}

	public DirectoryReader openReaderIfChanged(DirectoryReader oldReader) throws IOException {
		return DirectoryReader.openIfChanged( oldReader, delegate );
	}

	void close() throws IOException {
		delegate.close();
	}

	private void doCommit() throws IOException {
		delegate.commit();
		updateCommitExpiration();
	}

	private void updateCommitExpiration() {
		commitExpiration = commitInterval == 0 ? 0L : timingSource.getMonotonicTimeEstimate() + commitInterval;
	}

	private long getTimeToCommit() {
		return commitInterval == 0 ? 0L : commitExpiration - timingSource.getMonotonicTimeEstimate();
	}
}
