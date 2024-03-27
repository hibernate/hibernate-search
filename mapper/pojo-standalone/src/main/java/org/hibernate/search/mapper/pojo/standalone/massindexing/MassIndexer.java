/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.standalone.massindexing;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingFailureHandler;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.standalone.loading.MassLoadingOptions;
import org.hibernate.search.util.common.annotation.Incubating;

/**
 * A MassIndexer is useful to rebuild the indexes from the
 * data contained in the database.
 * This process is expensive: all indexed entities and their
 * indexedEmbedded properties are scrolled from database.
 *
 * @author Sanne Grinovero
 */
@Incubating
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
	 * Defaults to {@code true} for indexes that support it, {@code false} for other indexes.
	 * <p>
	 * This setting has no effect if {@code purgeAllOnStart} is set to false.
	 * @param enable {@code true} to enable this operation, {@code false} to disable it.
	 * @return {@code this} for method chaining
	 */
	MassIndexer mergeSegmentsAfterPurge(boolean enable);

	/**
	 * Drops the indexes and their schema (if they exist) and re-creates them before indexing.
	 * <p>
	 * Indexes will be unavailable for a short time during the dropping and re-creation,
	 * so this should only be used when failures of concurrent operations on the indexes (indexing caused by entity changes, ...)
	 * are acceptable.
	 * <p>
	 * This should be used when the existing schema is known to be obsolete, for example when the Hibernate Search mapping
	 * changed and some fields now have a different type, a different analyzer, new capabilities (projectable, ...), etc.
	 * <p>
	 * This may also be used when the schema is up-to-date,
	 * since it can be faster than a {@link #purgeAllOnStart(boolean) purge} on large indexes.
	 * <p>
	 * Defaults to {@code false}.
	 * @param dropAndCreateSchema if {@code true} the indexes and their schema will be dropped then re-created before starting the indexing
	 * @return {@code this} for method chaining
	 */
	MassIndexer dropAndCreateSchemaOnStart(boolean dropAndCreateSchema);

	/**
	 * Removes all entities from the indexes before indexing.
	 * <p>
	 * Set this to false only if you know there are no
	 * entities in the indexes: otherwise search results may be duplicated.
	 * <p>
	 * Default value depends on {@link #dropAndCreateSchemaOnStart(boolean)}. Defaults to {@code false} if the mass indexer
	 * is configured to drop and create the schema on start, to {@code true} otherwise.
	 * @param purgeAll if {@code true} all entities will be removed from the indexes before starting the indexing
	 * @return {@code this} for method chaining
	 */
	MassIndexer purgeAllOnStart(boolean purgeAll);

	/**
	 * Starts the indexing process in background (asynchronous).
	 * <p>
	 * May only be called once.
	 * @return a {@link java.util.concurrent.CompletionStage} to react to the completion of the indexing task.
	 * Call {@link CompletionStage#toCompletableFuture()} on the returned object
	 * to convert it to a {@link CompletableFuture} (which implements {@link java.util.concurrent.Future}).
	 */
	CompletionStage<?> start();

	/**
	 * Starts the indexing process, and then block until it's finished.
	 * <p>
	 * May only be called once.
	 * @throws InterruptedException if the current thread is interrupted
	 * while waiting.
	 */
	void startAndWait() throws InterruptedException;

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

	/**
	 * Sets the threshold for failures that will be reported and sent to {@link MassIndexingFailureHandler} per indexed type.
	 * Any failures exceeding this number will be ignored. A count of such ignored failures together with the operation
	 * they belong to will be reported to the failure handler upon the completion of indexing process.
	 *
	 * @param threshold The number of failures during one mass indexing beyond which the
	 * failure handler will no longer be notified. This threshold is reached separately for each indexed type.
	 * Overrides the {@link MassIndexingFailureHandler#failureFloodingThreshold() threshold defined by the failure handler itself}.
	 * <p>
	 * Defaults to {@code 100} with the default failure handler.
	 *
	 * @return {@code this} for method chaining
	 */
	@Incubating
	MassIndexer failureFloodingThreshold(long threshold);

	/**
	 * Sets context for use by the loading strategies.
	 * <p>
	 * The context can be retrieved through
	 * {@link MassLoadingOptions#context(Class)}.
	 *
	 * @param <T> The type of context.
	 * @param contextType The type of context, used as a key to retrieve it from
	 * {@link MassLoadingOptions#context(Class)}.
	 * @param context The context instance.
	 * @return {@code this} for method chaining
	 */
	<T> MassIndexer context(Class<T> contextType, T context);

	/**
	 * Sets the {@link MassIndexingEnvironment}, which can set up an environment (thread locals, ...) in mass indexing threads.
	 *
	 * @param environment a component that gets a chance to
	 * set up e.g. {@link ThreadLocal ThreadLocals} in mass indexing threads before
	 * mass indexing starts, and to remove them after mass indexing stops.
	 *
	 * @see MassIndexingEnvironment
	 */
	@Incubating
	MassIndexer environment(MassIndexingEnvironment environment);
}
