/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 * @param <E> The type of indexed entities.
 * @param <I> The type of identifiers.
 * @param <O> The type of mass indexing options.
 */
public class PojoMassIndexingBatchIndexingWorkspace<E, I, O> extends PojoMassIndexingFailureHandledRunnable {

	public static final String THREAD_NAME_PREFIX = "Mass indexing - ";

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final List<CompletableFuture<?>> identifierProducingFutures = new ArrayList<>();
	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();
	private final PojoMassIndexingMappingContext mappingContext;
	private final PojoMassIndexingIndexedTypeGroup<E, ?> typeGroup;
	private final PojoMassIndexingLoadingStrategy<E, I, O> loadingStrategy;
	private final O options;

	private final int entityExtractingThreads;

	PojoMassIndexingBatchIndexingWorkspace(PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingNotifier notifier,
			PojoMassIndexingIndexedTypeGroup<E, ?> typeGroup,
			PojoMassIndexingLoadingStrategy<E, I, O> loadingStrategy,
			O options, int entityExtractingThreads) {
		super( notifier );
		this.mappingContext = mappingContext;
		this.typeGroup = typeGroup;
		this.loadingStrategy = loadingStrategy;
		this.options = options;
		this.entityExtractingThreads = entityExtractingThreads;
	}

	@Override
	public void runWithFailureHandler() throws InterruptedException {
		if ( !identifierProducingFutures.isEmpty() || !indexingFutures.isEmpty() ) {
			throw new AssertionFailure( "BatchIndexingWorkspace instance not expected to be reused" );
		}

		PojoProducerConsumerQueue<List<I>> identifierQueue = new PojoProducerConsumerQueue<>( 1 );

		// First start the consumers, then the producers (reverse order):
		startIndexing( identifierQueue );
		startProducingPrimaryKeys( identifierQueue );
		// Wait for indexing to finish.
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf( indexingFutures.toArray( new CompletableFuture[0] ) )
		);
		log.debugf( "Indexing for %s is done", typeGroup.notifiedGroupName() );
	}

	@Override
	protected void cleanUpOnInterruption() {
		cancelPendingTasks();
	}

	@Override
	protected void cleanUpOnFailure() {
		cancelPendingTasks();
	}

	private void cancelPendingTasks() {
		// Cancel each pending task - threads executing the tasks must be interrupted
		for ( Future<?> task : identifierProducingFutures ) {
			task.cancel( true );
		}
		for ( Future<?> task : indexingFutures ) {
			task.cancel( true );
		}
	}

	private void startProducingPrimaryKeys(PojoProducerConsumerQueue<List<I>> identifierQueue) {
		final Runnable runnable = new PojoMassIndexingEntityIdentifierLoadingRunnable<>( getNotifier(), typeGroup,
				loadingStrategy, options, identifierQueue );
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		final ThreadPoolExecutor identifierProducingExecutor = mappingContext.threadPoolProvider().newFixedThreadPool(
				1,
				THREAD_NAME_PREFIX + typeGroup.notifiedGroupName() + " - ID loading"
		);
		try {
			identifierProducingFutures.add( Futures.runAsync( runnable, identifierProducingExecutor ) );
		}
		finally {
			identifierProducingExecutor.shutdown();
		}
	}

	private void startIndexing(PojoProducerConsumerQueue<List<I>> identifierQueue) {
		final Runnable runnable = new PojoMassIndexingEntityLoadingRunnable<>( getNotifier(), typeGroup,
				loadingStrategy, options, identifierQueue );
		final ThreadPoolExecutor indexingExecutor = mappingContext.threadPoolProvider().newFixedThreadPool(
				entityExtractingThreads,
				THREAD_NAME_PREFIX + typeGroup.notifiedGroupName() + " - Entity loading"
		);
		try {
			for ( int i = 0; i < entityExtractingThreads; i++ ) {
				indexingFutures.add( Futures.runAsync( runnable, indexingExecutor ) );
			}
		}
		finally {
			indexingExecutor.shutdown();
		}
	}
}
