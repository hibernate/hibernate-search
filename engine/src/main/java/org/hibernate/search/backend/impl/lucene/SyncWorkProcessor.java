/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * Processes changesets in batches, maintaining sync guarantees.
 *<p>
 * Multiple threads produce one or more {@link org.hibernate.search.backend.LuceneWork}
 * by calling {@link #submit(java.util.List, org.hibernate.search.backend.IndexingMonitor)},
 * and get blocked until their changes are applied to the index;</p>
 * The {@link org.hibernate.search.backend.impl.lucene.SyncWorkProcessor.Consumer} thread will
 * coalesce changes from multiple threads and apply them in the index, releasing the waiting threads
 * at the end.
 * <p>
 * In the absence of work to be applied, the Consumer thread is parked to avoid busy waiting.</p>
 *
 * @author gustavonalle
 */
final class SyncWorkProcessor implements WorkProcessor {

	private static final Log log = LoggerFactory.make();

	private final MultiWriteDrainableLinkedList<Changeset> transferQueue = new MultiWriteDrainableLinkedList<>();

	private volatile LuceneBackendResources resources;

	//To allow others to wait on actual shutdown of the internal threads
	private final CountDownLatch shutdownLatch = new CountDownLatch( 1 );

	private final String indexName;
	private volatile boolean stop = false;
	final Thread consumerThread;

	/**
	 * Constructor
	 * @param resources LuceneResources to obtain the workspace
	 * @param indexName for debugging purposes
	 */
	public SyncWorkProcessor(LuceneBackendResources resources, String indexName) {
		this.resources = resources;
		this.indexName = indexName;
		consumerThread = new Thread( new Consumer(), "Hibernate Search sync consumer thread for index " + indexName );
		consumerThread.setDaemon( true );
	}

	/**
	 * Start processing
	 */
	public void start() {
		log.startingSyncConsumerThread( indexName );
		consumerThread.start();
	}

	/**
	 * Submit work and wait for it to be applied to the index
	 * @param workList list of work
	 * @param monitor for statistics collection
	 */
	public void submit(List<LuceneWork> workList, IndexingMonitor monitor) {
		//avoid empty work lists as workaround for HSEARCH-1769
		if ( workList.isEmpty() ) {
			// only log this error at trace level until we properly fix HSEARCH-1769
			if ( log.isTraceEnabled() ) {
				StringWriter stackTraceStringWriter = new StringWriter();
				PrintWriter stackTracePrintWriter = new PrintWriter( stackTraceStringWriter );
				new Throwable().printStackTrace( stackTracePrintWriter );
				log.workListShouldNeverBeEmpty( stackTraceStringWriter.toString() );
			}
			// skip that work
			return;
		}
		Changeset changeset = new Changeset( workList, Thread.currentThread(), monitor );
		transferQueue.add( changeset );
		wakeUpConsumer();
		boolean interrupted = false;
		while ( ! changeset.isProcessed() && ! interrupted ) {
			parkCurrentThread();
			if ( Thread.interrupted() ) {
				interrupted = true;
			}
		}
		if ( interrupted ) {
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Wakes up consumer thread if necessary
	 */
	private void wakeUpConsumer() {
		LockSupport.unpark( consumerThread );
	}

	/**
	 * Dispose resources
	 */
	public void shutdown() {
		stop = true;
		LockSupport.unpark( consumerThread );
		try {
			shutdownLatch.await( Long.MAX_VALUE, TimeUnit.SECONDS );
		}
		catch (InterruptedException e) {
			log.timedOutWaitingShutdown( indexName );
		}
	}

	/**
	 * Handle on the fly rebuilds
	 * @param resources new instance of {@link org.hibernate.search.backend.impl.lucene.LuceneBackendResources}
	 */
	public void updateResources(LuceneBackendResources resources) {
		this.resources = resources;
	}

	/**
	 * Consumer thread
	 */
	private class Consumer implements Runnable {
		@Override
		public void run() {
			Iterable<Changeset> changesets;
			try {
				while ( !stop ) {
					changesets = transferQueue.drainToDetachedIterable();
					while ( changesets == null && !stop ) {
						// Avoid busy wait
						parkCurrentThread();
						changesets = transferQueue.drainToDetachedIterable();
					}
					if ( changesets != null ) {
						applyChangesets( changesets );
					}
				}
				log.stoppingSyncConsumerThread( indexName );
			}
			finally {
				shutdownLatch.countDown();
			}
		}

		private void applyChangesets(Iterable<Changeset> changesets) {
			ChangesetList changesetList = new ChangesetList( changesets );
			try {
				LuceneBackendQueueTask luceneBackendQueueTask = new LuceneBackendQueueTask( changesetList, resources, null );
				luceneBackendQueueTask.run();
			}
			finally {
				changesetList.markProcessed();
			}
		}
	}

	private void parkCurrentThread() {
		//Always use some safety margin when parking threads:
		LockSupport.parkNanos( 1_000_000_000 );
	}

}
