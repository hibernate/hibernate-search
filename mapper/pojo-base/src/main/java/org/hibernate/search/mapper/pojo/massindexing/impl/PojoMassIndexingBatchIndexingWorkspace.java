/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.MassIndexingLog;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingMappingContext;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 * @param <E> The type of indexed entities.
 * @param <I> The type of identifiers.
 */
public class PojoMassIndexingBatchIndexingWorkspace<E, I> extends PojoMassIndexingFailureHandledRunnable {

	public static final String THREAD_NAME_PREFIX = "Mass indexing - ";

	private final List<CompletableFuture<?>> identifierProducingFutures = new ArrayList<>();
	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();
	private final PojoMassIndexingMappingContext mappingContext;
	private final PojoMassIndexingIndexedTypeGroup<E> typeGroup;
	private final PojoMassLoadingStrategy<E, I> loadingStrategy;

	private final int entityExtractingThreads;
	private final String tenantId;
	private final MassIndexingTypeGroupMonitor typeGroupMonitor;
	private final MassIndexingTypeGroupContext<E> massIndexingTypeGroupContext;

	PojoMassIndexingBatchIndexingWorkspace(PojoMassIndexingMappingContext mappingContext,
			PojoMassIndexingNotifier notifier,
			MassIndexingEnvironment environment,
			PojoMassIndexingIndexedTypeGroup<E> typeGroup,
			PojoMassLoadingStrategy<E, I> loadingStrategy,
			PojoMassIndexingContext massIndexingContext,
			int entityExtractingThreads, String tenantId) {
		super( notifier, environment );
		this.mappingContext = mappingContext;
		this.typeGroup = typeGroup;
		this.loadingStrategy = loadingStrategy;
		this.entityExtractingThreads = entityExtractingThreads;
		this.tenantId = tenantId;
		this.massIndexingTypeGroupContext = new MassIndexingTypeGroupContext<>( typeGroup, massIndexingContext, tenantId );
		this.typeGroupMonitor = notifier.typeGroupMonitor( massIndexingTypeGroupContext );
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
		List<CompletableFuture<?>> allFutures = new ArrayList<>();
		allFutures.addAll( identifierProducingFutures );
		allFutures.addAll( indexingFutures );
		Futures.unwrappedExceptionGet( Futures.firstFailureOrAllOf( allFutures ) );
		typeGroupMonitor.indexingCompleted( massIndexingTypeGroupContext );
		MassIndexingLog.INSTANCE.indexingForTypeGroupDone( typeGroup.notifiedGroupName() );
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
		final Runnable runnable = new PojoMassIndexingEntityIdentifierLoadingRunnable<>(
				getNotifier(),
				typeGroupMonitor,
				massIndexingTypeGroupContext, getMassIndexingEnvironment(),
				typeGroup, loadingStrategy,
				identifierQueue
		);
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
		final Runnable runnable = new PojoMassIndexingEntityLoadingRunnable<>(
				getNotifier(),
				typeGroupMonitor,
				massIndexingTypeGroupContext.massIndexingContext(), getMassIndexingEnvironment(),
				typeGroup, loadingStrategy,
				identifierQueue, tenantId
		);
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
