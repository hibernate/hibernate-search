/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.lowlevel.writer.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.orchestration.spi.SingletonTask;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.impl.Closer;
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

	private final SingletonTask delayedCommitTask;
	private final ReentrantLock commitLock = new ReentrantLock();

	private long commitExpiration;

	public IndexWriterDelegatorImpl(IndexWriter delegate, EventContext eventContext,
			SimpleScheduledExecutor delayedCommitExecutor,
			TimingSource timingSource, int commitInterval,
			FailureHandler failureHandler,
			DelayedCommitFailureHandler delayedCommitFailureHandler) {
		this.delegate = delegate;
		this.eventContext = eventContext;
		this.timingSource = timingSource;
		this.commitInterval = commitInterval;
		this.failureHandler = failureHandler;

		if ( commitInterval == 0L ) {
			delayedCommitTask = null;
		}
		else {
			delayedCommitTask = new SingletonTask(
					"Delayed commit for " + eventContext.render(),
					new LuceneDelayedCommitWorker( delayedCommitFailureHandler ),
					new LuceneDelayedCommitScheduler( delayedCommitExecutor ),
					failureHandler
			);
		}

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

	public void commitOrDelay() {
		if ( !delegate.hasUncommittedChanges() ) {
			// No need to either commit or plan a delayed commit: there's nothing to commit.
			return;
		}

		if ( delayCommit() ) {
			// The commit was delayed
			return;
		}

		// Synchronize in order to prevent a scenario where two threads call commitOrDelay() concurrently,
		// both notice the previous commit has expired, and both trigger a commit,
		// resulting in two commits where one would have been enough.
		commitLock.lock();
		try {
			if ( delayCommit() ) {
				// The commit was delayed
				return;
			}

			// The previous commit has expired
			doCommit();
		}
		finally {
			commitLock.unlock();
		}
	}

	public DirectoryReader openReader() throws IOException {
		return DirectoryReader.open( delegate );
	}

	public DirectoryReader openReaderIfChanged(DirectoryReader oldReader) throws IOException {
		return DirectoryReader.openIfChanged( oldReader, delegate );
	}

	public IndexWriter getDelegateForTests() {
		return delegate;
	}

	void close() throws IOException {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( SingletonTask::stop, delayedCommitTask );
			// Avoid problems with closing while a (delayed) commit is in progress:
			// Lucene throws an exception in that case.
			commitLock.lock();
			try {
				closer.push( IndexWriter::close, delegate );
			}
			finally {
				commitLock.unlock();
			}
			log.trace( "IndexWriter closed" );
		}
	}

	void closeAfterFailure(Throwable throwable, Object failingOperation) {
		Exception exceptionToReport =
				log.uncommittedOperationsBecauseOfFailure( throwable.getMessage(), eventContext, throwable );
		try {
			close();
		}
		catch (RuntimeException | IOException e) {
			exceptionToReport.addSuppressed(
					log.unableToCloseIndexWriterAfterFailures( e.getMessage(), eventContext, e ) );
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
		commitLock.lock();
		try {
			// NOTE: underlying Lucene code is using this pattern to sync on object block,
			// which could be a problem with Loom:
			// synchronized(commitLock)
			delegate.commit();
			updateCommitExpiration();
		}
		catch (RuntimeException | IOException e) {
			throw log.unableToCommitIndex( e.getMessage(), eventContext, e );
		}
		finally {
			commitLock.unlock();
		}
	}

	/**
	 * @return {@code true} if the commit was delayed, {@code false} if it wasn't and must happen now.
	 */
	private boolean delayCommit() {
		long timeToCommit = getTimeToCommit();
		if ( timeToCommit <= 0L ) {
			// The commit must happen now.
			return false;
		}

		// There's still time before we must commit.
		// Just make sure the commit will happen eventually.
		delayedCommitTask.ensureScheduled();
		return true;
	}

	private long getTimeToCommit() {
		if ( commitInterval == 0L ) {
			// We never delay anything in this case,
			// so there's no need to query the timing source (which is probably null in this case).
			return 0L;
		}

		return commitExpiration - timingSource.monotonicTimeEstimate();
	}

	private void updateCommitExpiration() {
		commitExpiration = commitInterval == 0 ? 0L : timingSource.monotonicTimeEstimate() + commitInterval;
	}

	private class LuceneDelayedCommitWorker implements SingletonTask.Worker {
		private final CompletableFuture<?> completedFuture = CompletableFuture.completedFuture( null );
		private final DelayedCommitFailureHandler delayedCommitFailureHandler;

		public LuceneDelayedCommitWorker(DelayedCommitFailureHandler delayedCommitFailureHandler) {
			this.delayedCommitFailureHandler = delayedCommitFailureHandler;
		}

		@Override
		public CompletableFuture<?> work() {
			try {
				// This will re-schedule the task if it's still not time for a commit.
				commitOrDelay();
			}
			catch (Throwable t) {
				delayedCommitFailureHandler.handle( t, "Delayed commit" );
			}
			return completedFuture;
		}

		@Override
		public void complete() {
			// Called when commitOrDelay() returns 0: nothing to do.
		}
	}

	private class LuceneDelayedCommitScheduler implements SingletonTask.Scheduler {
		private final SimpleScheduledExecutor delegate;

		private LuceneDelayedCommitScheduler(SimpleScheduledExecutor delegate) {
			this.delegate = delegate;
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			// Schedule the task for execution as soon as the last commit expires.
			return delegate.schedule( runnable, getTimeToCommit(), TimeUnit.MILLISECONDS );
		}
	}

	interface DelayedCommitFailureHandler {

		void handle(Throwable throwable, Object failingOperation);

	}
}
