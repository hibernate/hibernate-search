/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.search.engine.reporting.spi.RootFailureCollector;
import org.hibernate.search.mapper.pojo.logging.impl.PojoEventContextMessages;
import org.hibernate.search.mapper.pojo.schema.management.spi.PojoScopeSchemaManager;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;

/**
 * Makes sure that several different BatchIndexingWorkspace(s)
 * can be started concurrently, sharing the same batch-backend
 * and IndexWriters.
 *
 * @author Sanne Grinovero
 */
public class PojoMassIndexingBatchCoordinator extends PojoMassIndexingFailureHandledRunnable {

	private final List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex;

	private final PojoScopeSchemaManager scopeSchemaManager;
	private final PojoScopeWorkspace scopeWorkspace;

	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();
	private final PojoMassIndexingMappingContext mappingContext;
	private final int typesToIndexInParallel;
	private final int documentBuilderThreads;
	private final boolean mergeSegmentsOnFinish;
	private final boolean dropAndCreateSchemaOnStart;
	private final boolean purgeAtStart;
	private final boolean mergeSegmentsAfterPurge;

	public PojoMassIndexingBatchCoordinator(PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingNotifier notifier,
			List<PojoMassIndexingIndexedTypeGroup<?>> typeGroupsToIndex,
			PojoScopeSchemaManager scopeSchemaManager, PojoScopeWorkspace scopeWorkspace,
			int typesToIndexInParallel, int documentBuilderThreads, boolean mergeSegmentsOnFinish,
			boolean dropAndCreateSchemaOnStart, boolean purgeAtStart, boolean mergeSegmentsAfterPurge) {
		super( notifier );
		this.mappingContext = mappingContext;
		this.typeGroupsToIndex = typeGroupsToIndex;

		this.scopeSchemaManager = scopeSchemaManager;
		this.scopeWorkspace = scopeWorkspace;
		this.typesToIndexInParallel = typesToIndexInParallel;
		this.documentBuilderThreads = documentBuilderThreads;
		this.mergeSegmentsOnFinish = mergeSegmentsOnFinish;
		this.dropAndCreateSchemaOnStart = dropAndCreateSchemaOnStart;
		this.purgeAtStart = purgeAtStart;
		this.mergeSegmentsAfterPurge = mergeSegmentsAfterPurge;
	}

	@Override
	public void runWithFailureHandler() throws InterruptedException {
		if ( !indexingFutures.isEmpty() ) {
			throw new AssertionFailure( "BatchCoordinator instance not expected to be reused" );
		}

		beforeBatch(); // purgeAll and mergeSegments if enabled
		try {
			doBatchWork();
			afterBatch(); // mergeSegments if enabled and flush
		}
		catch (MassIndexingOperationHandledFailureException e) {
			// Something is wrong, but it's already been reported.
			// Just stop everything and rely on the notifier to throw the appropriate exception.
			cleanUpOnFailure();
		}
	}

	@Override
	protected void cleanUpOnInterruption() throws InterruptedException {
		cancelPendingTasks();
		// Indexing performed before the exception must still be committed,
		// in order to leave the index in a consistent state
		afterBatchOnInterruption();
	}

	@Override
	protected void cleanUpOnFailure() {
		cancelPendingTasks();
	}

	@Override
	protected void notifySuccess() {
		getNotifier().reportIndexingCompleted();
	}

	@Override
	protected void notifyInterrupted(InterruptedException exception) {
		getNotifier().reportInterrupted( exception );
		getNotifier().reportIndexingCompleted();
	}

	@Override
	protected void notifyFailure(RuntimeException exception) {
		super.notifyFailure( exception );
		// TODO HSEARCH-3729 Call a different method when indexing failed?
		getNotifier().reportIndexingCompleted();
	}

	private void cancelPendingTasks() {
		for ( Future<?> task : indexingFutures ) {
			if ( !task.isDone() ) {
				task.cancel( true );
			}
		}
	}

	/**
	 * Will spawn a thread for each type in rootEntities, they will all re-join
	 * on endAllSignal when finished.
	 *
	 * @throws InterruptedException if interrupted while waiting for endAllSignal.
	 */
	private void doBatchWork() throws InterruptedException {
		ExecutorService executor = mappingContext.threadPoolProvider()
				.newFixedThreadPool( typesToIndexInParallel,
						PojoMassIndexingBatchIndexingWorkspace.THREAD_NAME_PREFIX + "Workspace" );

		for ( PojoMassIndexingIndexedTypeGroup<?> typeGroup : typeGroupsToIndex ) {
			indexingFutures.add( Futures.runAsync( createBatchIndexingWorkspace( typeGroup ), executor ) );
		}
		executor.shutdown();

		// Wait for the executor to finish
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf( indexingFutures.toArray( new CompletableFuture[0] ) )
		);
	}

	private <E> PojoMassIndexingBatchIndexingWorkspace<E, ?> createBatchIndexingWorkspace(
			PojoMassIndexingIndexedTypeGroup<E> typeGroup) {
		return new PojoMassIndexingBatchIndexingWorkspace<>(
				mappingContext, getNotifier(), typeGroup,
				typeGroup.loadingStrategy(),
				documentBuilderThreads );
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 */
	private void afterBatch() throws InterruptedException {
		if ( mergeSegmentsOnFinish ) {
			Futures.unwrappedExceptionGet( scopeWorkspace.mergeSegments() );
		}
		Futures.unwrappedExceptionGet( scopeWorkspace.flush() );
		Futures.unwrappedExceptionGet( scopeWorkspace.refresh() );
	}

	/**
	 * batch indexing has been interrupted : flush to apply all index update realized before interruption
	 */
	private void afterBatchOnInterruption() throws InterruptedException {
		Futures.unwrappedExceptionGet( scopeWorkspace.flush() );
		Futures.unwrappedExceptionGet( scopeWorkspace.refresh() );
	}

	/**
	 * Optional operations to do before the multiple-threads start indexing
	 */
	private void beforeBatch() throws InterruptedException {
		if ( dropAndCreateSchemaOnStart ) {
			RootFailureCollector failureCollector = new RootFailureCollector(
					PojoEventContextMessages.INSTANCE.schemaManagement()
			);
			Futures.unwrappedExceptionGet( scopeSchemaManager.dropAndCreate( failureCollector ) );
			failureCollector.checkNoFailure();
		}

		if ( purgeAtStart ) {
			Futures.unwrappedExceptionGet( scopeWorkspace.purge( Collections.emptySet() ) );
			if ( mergeSegmentsAfterPurge ) {
				Futures.unwrappedExceptionGet( scopeWorkspace.mergeSegments() );
			}
		}
	}

}
