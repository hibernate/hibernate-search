package org.hibernate.search;

import java.util.concurrent.Future;

import org.hibernate.CacheMode;

/**
 * A MassIndexer is useful to rebuild the indexes from the
 * data contained in the database.
 * This process is expensive: all indexed entities and their
 * indexedEmbedded properties are scrolled from database.
 * 
 * @author Sanne Grinovero
 */
public interface MassIndexer {
	
	/**
	 * Set the number of threads to be used to load
	 * the root entities.
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer threadsToLoadObjects(int numberOfThreads);
	
	/**
	 * Sets the batch size used to load the root entities.
	 * @param batchSize
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer batchSizeToLoadObjects(int batchSize);
	
	/**
	 * Sets the number of threads used to load the lazy collections
	 * related to the indexed entities.
	 * @param numberOfThreads
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer threadsForSubsequentFetching(int numberOfThreads);
	
	/**
	 * Sets the number of threads to be used to analyze the documents
	 * and write to the index.
	 * @param numberOfThreads
	 * @return
	 */
	//TODO implement? performance improvement was found to be
	//interesting in unusual setups only.
	//MassIndexer threadsForIndexWriter(int numberOfThreads);
	
	/**
	 * Sets the cache interaction mode for the data loading tasks.
	 * Defaults to <tt>CacheMode.IGNORE</tt>.
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer cacheMode(CacheMode cacheMode);
	
	/**
	 * If index optimization has to be started at the end
	 * of the indexing process.
	 * Defaults to <tt>true</tt>.
	 * @param optimize
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer optimizeOnFinish(boolean optimize);
	
	/**
	 * If index optimization should be run before starting,
	 * after the purgeAll. Has no effect if <tt>purgeAll</tt> is set to false.
	 * Defaults to <tt>true</tt>.
	 * @param optimize
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer optimizeAfterPurge(boolean optimize);
	
	/**
	 * If all entities should be removed from the index before starting
	 * using purgeAll. Set it to false only if you know there are no
	 * entities in the index: otherwise search results may be duplicated.
	 * Defaults to true.
	 * @param purgeAll
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer purgeAllOnStart(boolean purgeAll);
	
	/**
	 * EXPERIMENTAL method: will probably change
	 * 
	 * Will stop indexing after having indexed a set amount of objects.
	 * As a results the index will not be consistent
	 * with the database: use only for testing on an (undefined) subset of database data.
	 * @param maximum
	 * @return
	 */
	MassIndexer limitIndexedObjectsTo(int maximum);

	/**
	 * Starts the indexing process in background (asynchronous).
	 * Can be called only once.
	 * @return a Future to control task canceling.
	 * get() will block until completion.
	 * cancel() is currently not implemented.
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
