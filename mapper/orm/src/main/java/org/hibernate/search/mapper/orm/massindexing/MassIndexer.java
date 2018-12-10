/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

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
	 * Sets the number of entity types to be indexed in parallel.
	 * Defaults to 1.
	 *
	 * @param threadsToIndexObjects  number of entity types to be indexed in parallel
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer typesToIndexInParallel(int threadsToIndexObjects);

	/**
	 * Set the number of threads to be used to load
	 * the root entities.
	 * @param numberOfThreads the number of threads
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer threadsToLoadObjects(int numberOfThreads);

	/**
	 * Sets the batch size used to load the root entities.
	 * @param batchSize the batch size
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer batchSizeToLoadObjects(int batchSize);

	/**
	 * Sets the cache interaction mode for the data loading tasks.
	 * Defaults to <tt>CacheMode.IGNORE</tt>.
	 * @param cacheMode the cache interaction mode
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer cacheMode(CacheMode cacheMode);

	/**
	 * If index optimization has to be started at the end of the indexing process. Defaults to <tt>true</tt>.
	 * @param optimize {@code true} to enable the index optimization at the end of the indexing process
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer optimizeOnFinish(boolean optimize);

	/**
	 * If index optimization should be run before starting,
	 * after the purgeAll. Has no effect if <tt>purgeAll</tt> is set to false.
	 * Defaults to <tt>true</tt>.
	 * @param optimize {@code true} to enable the index optimization after purge
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer optimizeAfterPurge(boolean optimize);

	/**
	 * If all entities should be removed from the index before starting
	 * using purgeAll. Set it to false only if you know there are no
	 * entities in the index: otherwise search results may be duplicated.
	 * Defaults to true.
	 * @param purgeAll if {@code true} all entities will be removed from the index before starting the indexing
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer purgeAllOnStart(boolean purgeAll);

	/**
	 * EXPERIMENTAL method: will probably change
	 *
	 * Will stop indexing after having indexed a set amount of objects.
	 * As a results the index will not be consistent
	 * with the database: use only for testing on an (undefined) subset of database data.
	 * @param maximum the maximum number of objects to index
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer limitIndexedObjectsTo(long maximum);

	/**
	 * Starts the indexing process in background (asynchronous).
	 * Can be called only once.
	 * @return a Future to control the indexing task.
	 */
	Future<?> start();

	/**
	 * Starts the indexing process, and then block until it's finished.
	 * Can be called only once.
	 * @throws InterruptedException if the current thread is interrupted
	 * while waiting.
	 */
	void startAndWait() throws InterruptedException;

	/**
	 * Specifies the fetch size to be used when loading primary keys
	 * if objects to be indexed. Some databases accept special values,
	 * for example MySQL might benefit from using {@link Integer#MIN_VALUE}
	 * otherwise it will attempt to preload everything in memory.
	 * @param idFetchSize the fetch size to be used when loading primary keys
	 * @return <tt>this</tt> for method chaining
	 */
	MassIndexer idFetchSize(int idFetchSize);

	/**
	 * Timeout of transactions for loading ids and entities to be re-indexed. Specify a timeout which is long enough to
	 * load and index all entities of the type with the most instances, taking into account the configured batch size
	 * and number of threads to load objects.
	 * <p>
	 * Only supported in JTA-compatible environments.
	 *
	 * @param timeoutInSeconds the transaction timeout in seconds; If no value is given, the global default timeout of
	 * the JTA environment applies.
	 * @return {@code this} for method chaining
	 */
	MassIndexer transactionTimeout(int timeoutInSeconds);
}
