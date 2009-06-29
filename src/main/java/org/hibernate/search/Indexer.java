package org.hibernate.search;

import java.util.concurrent.Future;

import org.hibernate.CacheMode;

public interface Indexer {
	
	/**
	 * Set the number of threads to be used to load
	 * the root entities.
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer objectLoadingThreads(int numberOfThreads);
	
	/**
	 * Sets the batch size used to load the root entities.
	 * @param batchSize
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer objectLoadingBatchSize(int batchSize);
	
	/**
	 * Sets the number of threads used to load the lazy collections
	 * related to the indexed entities.
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer documentBuilderThreads(int numberOfThreads);
	
	/**
	 * Sets the number of threads to be used to analyze the documents
	 * and write to the index.
	 * @param numberOfThreads
	 * @return
	 */
	//TODO not yet implemented
	//Indexer indexWriterThreads(int numberOfThreads);
	
	/**
	 * Sets the cache interaction mode for the data loading tasks.
	 * Defaults to <tt>CacheMode.IGNORE</tt>.
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer cacheMode(CacheMode cacheMode);
	
	/**
	 * If index optimization has to be started at the end
	 * of the indexing process.
	 * Defaults to <tt>true</tt>.
	 * @param optimize
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer optimizeAtEnd(boolean optimize);
	
	/**
	 * If index optimization should be run before starting,
	 * after the purgeAll. Has no effect if <tt>purgeAll</tt> is set to false.
	 * Defaults to <tt>true</tt>.
	 * @param optimize
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer optimizeAfterPurge(boolean optimize);
	
	/**
	 * If all entities should be removed from the index before starting
	 * using purgeAll. Set it only to false if you know there are no
	 * entities in the index: otherwise search results may be duplicated.
	 * Defaults to true.
	 * @param purgeAll
	 * @return <tt>this</tt> for method chaining
	 */
	Indexer purgeAllAtStart(boolean purgeAll);
	
	/**
	 * Will stop indexing after having indexed this amount of objects.
	 * As a results the index will not be consistent
	 * with the database: use only for testing.
	 * @param maximum
	 * @return
	 */
	Indexer limitObjects(int maximum);

	/**
	 * Starts the indexing process in background (asynchronous).
	 * Can be called only once.
	 * @return a Future to control task canceling. get() will always return null,
	 * blocking until completion.
	 */
	Future<?> start();
	
	/**
	 * Starts the indexing process, and then block until it's finished.
	 * Can be called only once.
	 * @throws InterruptedException if the current thread is interrupted
     * while waiting.
	 */
	void startAndWait() throws InterruptedException;

}
