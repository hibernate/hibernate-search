/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing;

import java.util.concurrent.CompletableFuture;

import org.hibernate.CacheMode;
import org.hibernate.search.util.common.annotaion.Incubating;

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
	 * <p>
	 * Defaults to {@code 1}.
	 *
	 * @param threadsToIndexObjects  number of entity types to be indexed in parallel
	 * @return {@code this} for method chaining
	 */
	MassIndexer typesToIndexInParallel(int threadsToIndexObjects);

	/**
	 * Sets the number of threads to be used to load
	 * the root entities.
	 * @param numberOfThreads the number of threads
	 * @return {@code this} for method chaining
	 */
	MassIndexer threadsToLoadObjects(int numberOfThreads);

	/**
	 * Sets the batch size used to load the root entities.
	 * @param batchSize the batch size
	 * @return {@code this} for method chaining
	 */
	MassIndexer batchSizeToLoadObjects(int batchSize);

	/**
	 * Sets the cache interaction mode for the data loading tasks.
	 * <p>
	 * Defaults to {@code CacheMode.IGNORE}.
	 * @param cacheMode the cache interaction mode
	 * @return {@code this} for method chaining
	 */
	MassIndexer cacheMode(CacheMode cacheMode);

	/**
	 * Merges each index into a single segment after indexing.
	 * <p>
	 * Defaults to {@code false}.
	 * @param enable {@code true} to enable this operation, {@code false} to disable it.
	 * @return {@code this} for method chaining
	 */
	MassIndexer mergeSegmentsOnFinish(boolean enable);

	/**
	 * Merges each index into a single segment after the initial index purge, just before indexing.
	 * <p>
	 * Defaults to {@code true}.
	 * <p>
	 * This setting has no effect if {@code purgeAllOnStart} is set to false.
	 * @param enable {@code true} to enable this operation, {@code false} to disable it.
	 * @return {@code this} for method chaining
	 */
	MassIndexer mergeSegmentsAfterPurge(boolean enable);

	/**
	 * Removes all entities from the indexes before indexing.
	 * <p>
	 * Set this to false only if you know there are no
	 * entities in the indexes: otherwise search results may be duplicated.
	 * <p>
	 * Defaults to {@code true}.
	 * @param purgeAll if {@code true} all entities will be removed from the indexes before starting the indexing
	 * @return {@code this} for method chaining
	 */
	MassIndexer purgeAllOnStart(boolean purgeAll);

	/**
	 * Stops indexing after having indexed a set amount of objects.
	 * <p>
	 * As a results the indexes will not be consistent
	 * with the database: use only for testing on an (undefined) subset of database data.
	 * @param maximum the maximum number of objects to index
	 * @return {@code this} for method chaining
	 */
	@Incubating
	MassIndexer limitIndexedObjectsTo(long maximum);

	/**
	 * Starts the indexing process in background (asynchronous).
	 * <p>
	 * May only be called once.
	 * @return a Future to control the indexing task.
	 */
	CompletableFuture<?> start();

	/**
	 * Starts the indexing process, and then block until it's finished.
	 * <p>
	 * May only be called once.
	 * @throws InterruptedException if the current thread is interrupted
	 * while waiting.
	 */
	void startAndWait() throws InterruptedException;

	/**
	 * Specifies the fetch size to be used when loading primary keys
	 * if objects to be indexed.
	 * <p>
	 * Some databases accept special values,
	 * for example MySQL might benefit from using {@link Integer#MIN_VALUE}
	 * otherwise it will attempt to preload everything in memory.
	 * @param idFetchSize the fetch size to be used when loading primary keys
	 * @return {@code this} for method chaining
	 */
	MassIndexer idFetchSize(int idFetchSize);

	/**
	 * Timeout of transactions for loading ids and entities to be re-indexed.
	 * <p>
	 * Specify a timeout which is long enough to
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

	/**
	 * Sets the {@link MassIndexingMonitor}.
	 * <p>
	 * The default monitor just logs the progress.
	 *
	 * @param monitor The monitor that will track mass indexing progress.
	 * @return {@code this} for method chaining
	 */
	MassIndexer monitor(MassIndexingMonitor monitor);

	/**
	 * Sets the {@link MassIndexingFailureHandler}.
	 * <p>
	 * The default handler just forwards failures to the
	 * {@link org.hibernate.search.engine.cfg.EngineSettings#BACKGROUND_FAILURE_HANDLER background failure handler}.
	 *
	 * @param failureHandler The handler for failures occurring during mass indexing.
	 * @return {@code this} for method chaining
	 */
	MassIndexer failureHandler(MassIndexingFailureHandler failureHandler);
}
