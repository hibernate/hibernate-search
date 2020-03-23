/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.timeout.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;

/**
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class IndexWriterDelegatorImpl implements IndexWriterDelegator {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexWriter delegate;
	private final EventContext eventContext;
	private final TimingSource timingSource;
	private final int commitInterval;
	private final FailureHandler failureHandler;

	private final Object commitOrDelayLock = new Object();

	private long commitExpiration;

	public IndexWriterDelegatorImpl(IndexWriter delegate, EventContext eventContext,
			TimingSource timingSource, int commitInterval,
			FailureHandler failureHandler) {
		this.delegate = delegate;
		this.eventContext = eventContext;
		this.timingSource = timingSource;
		this.commitInterval = commitInterval;
		this.failureHandler = failureHandler;
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

	public void commit() {
		doCommit();
	}

	public long commitOrDelay() {
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
		log.trace( "IndexWriter closed" );
	}

	public void closeAfterFailure(Throwable throwable, Object failingOperation) {
		Exception exceptionToReport = log.uncommittedOperationsBecauseOfFailure( throwable.getMessage(), eventContext, throwable );
		try {
			close();
		}
		catch (RuntimeException | IOException e) {
			exceptionToReport.addSuppressed( log.unableToCloseIndexWriterAfterFailures( eventContext, e ) );
		}

		/*
		 * The failing operation will be reported elsewhere,
		 * but that report will not mention that some previously executed,
		 * but uncommitted operations may have been affected too.
		 * Report the failure again, just to warn about previous operations potentially being affected.
		 */
		FailureContext.Builder failureContextBuilder = FailureContext.builder();
		failureContextBuilder.throwable( exceptionToReport );
		failureContextBuilder.failingOperation( failingOperation );
		FailureContext failureContext = failureContextBuilder.build();
		failureHandler.handle( failureContext );
	}

	private void doCommit() {
		try {
			delegate.commit();
			updateCommitExpiration();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCommitIndex( eventContext, e );
		}
	}

	private void updateCommitExpiration() {
		commitExpiration = commitInterval == 0 ? 0L : timingSource.getMonotonicTimeEstimate() + commitInterval;
	}

	private long getTimeToCommit() {
		return commitInterval == 0 ? 0L : commitExpiration - timingSource.getMonotonicTimeEstimate();
	}
}
