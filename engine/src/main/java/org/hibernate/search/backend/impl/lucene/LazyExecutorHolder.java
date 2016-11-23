/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

final class LazyExecutorHolder {

	private static final Log log = LoggerFactory.make();

	private final int maxQueueLength;
	private final String threadNamePrefix;
	private final String indexName;

	/**
	 * Read/Write locks to protect usage of the lazy initialized
	 * Async Indexing Executor.
	 * Typical executor usage is allowed under a shared lock.
	 * Starting/Stopping the executor itself requires an
	 * exclusive lock.
	 */
	private final ReadLock executorStateReadLock;
	private final WriteLock executorStateWriteLock;

	/**
	 * Lazily initialized; state change protected by executorStateWriteLock
	 */
	private ExecutorService asyncIndexingExecutor;

	public LazyExecutorHolder(int maxQueueLength, String indexName, String threadNamePrefix) {
		this.maxQueueLength = maxQueueLength;
		this.indexName = indexName;
		this.threadNamePrefix = threadNamePrefix;
		final ReentrantReadWriteLock executorStateReadWriteLock = new ReentrantReadWriteLock();
		this.executorStateReadLock = executorStateReadWriteLock.readLock();
		this.executorStateWriteLock = executorStateReadWriteLock.writeLock();
	}

	/**
	 * Submits a task to the asynchronous queue executor,
	 * which might get started if it wasn't started already.
	 * @param task
	 */
	public void submitTask(LuceneBackendQueueTask task) {
		executorStateReadLock.lock();
		try {
			final ExecutorService executor = asyncIndexingExecutor;
			if ( executor != null ) {
				executor.submit( task );
				return; // !
			}
		}
		finally {
			executorStateReadLock.unlock();
		}
		//If not returned yet, means the executor wasn't available;
		//Needs to be started within the exclusive lock.
		executorStateWriteLock.lock();
		try {
			ExecutorService executor = asyncIndexingExecutor;
			if ( executor == null ) {
				executor = Executors.newFixedThreadPool( 1, threadNamePrefix, maxQueueLength );
				this.asyncIndexingExecutor = executor;
			}
			executor.submit( task );
		}
		finally {
			executorStateWriteLock.unlock();
		}
	}

	public void flushCloseExecutor() {
		executorStateWriteLock.lock();
		try {
			if ( asyncIndexingExecutor == null ) {
				return;
			}
			asyncIndexingExecutor.shutdown();
			try {
				asyncIndexingExecutor.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
			}
			catch (InterruptedException e) {
				log.interruptedWhileWaitingForIndexActivity( e );
			}
			if ( ! asyncIndexingExecutor.isTerminated() ) {
				log.unableToShutdownAsynchronousIndexingByTimeout( indexName );
			}
			asyncIndexingExecutor = null;
		}
		finally {
			executorStateWriteLock.unlock();
		}
	}

	public int getMaxQueueLength() {
		return maxQueueLength;
	}

}
