/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Commit policy that will commit at a regular intervals defined by configuration or immediately on
 * explicit flush requests
 *
 * @author gustavonalle
 */
public final class ScheduledCommitPolicy extends AbstractCommitPolicy {

	public static final int DEFAULT_DELAY_MS = 1000;
	private static final Log log = LoggerFactory.make();

	private volatile ScheduledExecutorService scheduledExecutorService;
	private final ErrorHandler errorHandler;
	private final int delay;
	private final String indexName;
	private final AtomicBoolean running = new AtomicBoolean( false );

	public ScheduledCommitPolicy(IndexWriterHolder indexWriterHolder, String indexName, int delay, ErrorHandler errorHandler) {
		super( indexWriterHolder );
		this.indexName = indexName;
		this.delay = delay;
		this.errorHandler = errorHandler;
	}

	public int getDelay() {
		return delay;
	}

	@Override
	public void onChangeSetApplied(boolean someFailureHappened, boolean streaming) {
		if ( running.get() == false ) {
			startScheduledExecutor();
		}
		if ( someFailureHappened ) {
			indexWriterHolder.forceLockRelease();
		}
	}

	/**
	 * Exposed as public method for tests only
	 *
	 * @return The executor used by this policy, newly created if necessary.
	 */
	public synchronized ScheduledExecutorService getScheduledExecutorService() {
		if ( scheduledExecutorService == null ) {
			scheduledExecutorService = Executors.newScheduledThreadPool( "Commit Scheduler for index " + indexName );
		}
		return scheduledExecutorService;
	}

	@Override
	public void onFlush() {
		indexWriterHolder.commitIndexWriter();
	}

	@Override
	public void onClose() {
		if ( scheduledExecutorService != null ) {
			stopScheduledExecutor();
		}
	}

	private synchronized void stopScheduledExecutor() {
		if ( scheduledExecutorService == null ) {
			return;
		}
		try {
			scheduledExecutorService.shutdown();
			scheduledExecutorService.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
			running.set( false );
			scheduledExecutorService = null;
		}
		catch (InterruptedException e) {
			log.timedOutWaitingShutdown( indexName );
		}
	}

	private synchronized void startScheduledExecutor() {
		if ( running.get() ) {
			return;
		}
		getScheduledExecutorService().scheduleWithFixedDelay(
				new CommitTask(),
				delay,
				delay,
				TimeUnit.MILLISECONDS
		);
		running.set( true );
	}

	private final class CommitTask implements Runnable {

		@Override
		public void run() {
			// This is technically running in a race condition with a possible shutdown
			// (indexwriter getting closed), which would cause an AlreadyClosedException exception,
			// but gets swallowed as it's running in the service thread (which is also shutting down).
			try {
				indexWriterHolder.commitIndexWriter();
			}
			catch (Exception e) {
				errorHandler.handleException( "Error caught in background thread of ScheduledCommitPolicy", e );
			}
		}
	}

}
