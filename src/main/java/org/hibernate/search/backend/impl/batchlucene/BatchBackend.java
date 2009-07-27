//$Id$
package org.hibernate.search.backend.impl.batchlucene;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.batchindexing.IndexerProgressMonitor;
import org.hibernate.search.engine.SearchFactoryImplementor;

/**
 * Implementors of this interface are not drop-in replacements for the standard BackendQueueProcessorFactory
 * but are meant to be used only during batch processing.
 * The order of LuceneWork(s) processed is not guaranteed as the queue is consumed by several
 * concurrent workers. 
 *  
 * @author Sanne Grinovero
 */
public interface BatchBackend {
	
	/**
	 * Used at startup, called once as first method.
	 * @param props all configuration properties
	 * @param searchFactory the client
	 */
	void initialize(Properties props, IndexerProgressMonitor monitor, SearchFactoryImplementor searchFactory);

	/**
	 * Enqueues one work to be processed asynchronously
	 * @param work
	 * @throws InterruptedException if the current thread is interrupted while
	 * waiting for the work queue to have enough space.
	 */
	void enqueueAsyncWork(LuceneWork work) throws InterruptedException;
	
	/**
	 * Does one work in sync
	 * @param work
	 * @throws InterruptedException
	 */
	void doWorkInSync(LuceneWork work);
	
	/**
	 * Waits until all work is done and terminates the executors.
	 * IndexWriter is not closed yet: work in sync can still be processed.
	 * @throws InterruptedException if the current thread is interrupted
     * while waiting for the enqueued tasks to be finished.
	 */
	void stopAndFlush(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * Used to shutdown and release resources.
	 * No other method should be used after this one.
	 */
	void close();

}
