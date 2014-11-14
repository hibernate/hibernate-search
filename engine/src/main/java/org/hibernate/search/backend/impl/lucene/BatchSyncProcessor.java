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

import java.util.List;
import java.util.concurrent.locks.LockSupport;

/**
 * Processes changesets in batches, maintaining sync guarantees.
 *<p>
 * Multiple threads produce one or more {@link org.hibernate.search.backend.LuceneWork}
 * by calling {@link #submit(java.util.List, org.hibernate.search.backend.IndexingMonitor)},
 * and get blocked until their changes are applied to the index;</p>
 * The {@link org.hibernate.search.backend.impl.lucene.BatchSyncProcessor.Consumer} thread will
 * coalesce changes from multiple threads and apply them in the index, releasing the waiting threads
 * at the end.
 * <p>
 * In the absence of work to be applied, the Consumer thread is parked to avoid busy waiting.</p>
 *
 * @author gustavonalle
 */
public class BatchSyncProcessor implements BlockProcessor {

	private static final Log log = LoggerFactory.make();

	private final MultiWriteDrainableLinkedList<Changeset> transferQueue = new MultiWriteDrainableLinkedList<>();

	private volatile LuceneBackendResources resources;
	private final String indexName;
	private volatile boolean stop = false;
	final Thread consumerThread;

	/**
	 * Constructor
	 * @param resources LuceneResources to obtain the workspace
	 * @param indexName for debugging purposes
	 */
	public BatchSyncProcessor(LuceneBackendResources resources, String indexName) {
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
			while ( ! stop ) {
				while ( transferQueue.isEmpty() && ! stop ) {
					// Avoid busy wait
					parkCurrentThread();
				}
				if ( ! transferQueue.isEmpty() ) {
					Iterable<Changeset> changesets = transferQueue.drainToDetachedIterable();
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
			log.stoppingSyncConsumerThread( indexName );
		}
	}

	private void parkCurrentThread() {
		//Always use some safety margin when parking threads:
		LockSupport.parkNanos( 1_000_000_000 );
	}

}
