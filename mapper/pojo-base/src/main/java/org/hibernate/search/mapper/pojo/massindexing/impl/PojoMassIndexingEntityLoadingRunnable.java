/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingContext;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.MassIndexingLog;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingEnvironment;
import org.hibernate.search.mapper.pojo.massindexing.MassIndexingTypeGroupMonitor;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.reporting.impl.PojoMassIndexerMessages;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

public class PojoMassIndexingEntityLoadingRunnable<E, I> extends PojoMassIndexingFailureHandledRunnable {

	private final MassIndexingTypeGroupMonitor typeGroupMonitor;
	private final PojoMassIndexingContext massIndexingContext;
	private final PojoMassIndexingIndexedTypeGroup<E> typeGroup;
	private final PojoMassLoadingStrategy<E, I> loadingStrategy;
	private final PojoProducerConsumerQueue<List<I>> identifierQueue;
	private final String tenantId;
	private final MassIndexingEnvironment.EntityLoadingContext entityLoadingContext;

	protected PojoMassIndexingEntityLoadingRunnable(PojoMassIndexingNotifier notifier,
			MassIndexingTypeGroupMonitor typeGroupMonitor,
			PojoMassIndexingContext massIndexingContext, MassIndexingEnvironment environment,
			PojoMassIndexingIndexedTypeGroup<E> typeGroup,
			PojoMassLoadingStrategy<E, I> loadingStrategy,
			PojoProducerConsumerQueue<List<I>> identifierQueue,
			String tenantId) {
		super( notifier, environment );
		this.typeGroupMonitor = typeGroupMonitor;
		this.massIndexingContext = massIndexingContext;
		this.typeGroup = typeGroup;
		this.loadingStrategy = loadingStrategy;
		this.identifierQueue = identifierQueue;
		this.tenantId = tenantId;

		this.entityLoadingContext = new EntityLoadingContextImpl();
	}

	@Override
	protected void runWithFailureHandler() throws InterruptedException {
		MassIndexingLog.INSTANCE.entityLoadingStarted( typeGroup.notifiedGroupName() );
		LoadingContext context = new LoadingContext();
		try ( PojoMassEntityLoader<I> entityLoader =
				loadingStrategy.createEntityLoader( typeGroup.includedTypes(), context ) ) {
			List<I> idList;
			do {
				idList = identifierQueue.take();
				if ( idList != null ) {
					MassIndexingLog.INSTANCE.entityLoadingAttemptToLoadIds( idList );
					// This will pass the loaded entities to the sink, which will trigger indexing for those entities.
					try {
						entityLoader.load( idList );
					}
					catch (RuntimeException e) {
						getNotifier().reportEntitiesLoadingFailure( typeGroup, idList, e );
					}
				}
			}
			while ( idList != null );
			context.waitForLastBatches();
		}
		MassIndexingLog.INSTANCE.entityLoadingFinished( typeGroup.notifiedGroupName() );
	}

	@Override
	protected void cleanUpOnFailure() {
		// Nothing to do
	}

	@Override
	protected void cleanUpOnInterruption() {
		// Nothing to do
	}

	@Override
	protected MassIndexingEnvironment.Context createMassIndexingEnvironmentContext() {
		return entityLoadingContext;
	}

	@Override
	protected boolean supportsThreadLifecycleHooks() {
		return true;
	}

	@Override
	protected String operationName() {
		return PojoMassIndexerMessages.INSTANCE.massIndexingLoadingAndExtractingEntityData( typeGroup.notifiedGroupName() );
	}

	private final class LoadingContext implements PojoMassEntityLoadingContext<E> {
		// The traditional implementation was equivalent to using 1.
		// Theoretically we could raise this above 2, but it would only help
		// if loading performance is inconsistent, so as to provide a "buffer"
		// of ongoing indexing operations that the backend can go through
		// while the loader is catching up.
		// So, for now, 2 should be enough.
		private static final int CONCURRENT_BATCHES = 2;

		private final List<IndexingBatch> batches;
		private int currentBatchIndex = 0;

		public LoadingContext() {
			batches = new ArrayList<>( CONCURRENT_BATCHES );
			for ( int i = 0; i < CONCURRENT_BATCHES; i++ ) {
				batches.add( new IndexingBatch() );
			}
		}

		@Override
		public PojoMassLoadingContext parent() {
			return massIndexingContext;
		}

		@Override
		public PojoMassEntitySink<E> createSink(PojoMassIndexingSessionContext sessionContext) {
			PojoIndexer indexer = sessionContext.createIndexer();
			return new PojoMassEntitySink<E>() {
				@Override
				public void accept(List<? extends E> batch) throws InterruptedException {
					if ( batch == null || batch.isEmpty() ) {
						return;
					}
					IndexingBatch currentBatch = batches.get( currentBatchIndex );
					// Make sure we don't erase state about an ongoing batch:
					// wait for an ongoing batch to finish before we start a new one.
					currentBatch.waitForIndexingEndAndReport();
					// Start indexing the batch. Once this returns,
					// we know the batch of entities has been processed and turned into documents,
					// so we can safely call the loader again for the next batch,
					// even if the loader clears the session before each batch.
					currentBatch.startIndexingList( sessionContext, indexer, batch );
					currentBatchIndex = ( currentBatchIndex + 1 ) % CONCURRENT_BATCHES;
					// We will wait for indexing to finish either the next time this method is called,
					// or when waitForLastBatches() is called at the end.
				}
			};
		}

		@Override
		public String tenantIdentifier() {
			return tenantId;
		}

		public void waitForLastBatches() throws InterruptedException {
			for ( IndexingBatch batch : batches ) {
				batch.waitForIndexingEndAndReport();
			}
		}
	}

	private final class IndexingBatch {

		private PojoMassIndexingSessionContext sessionContext;
		private List<?> entities;
		private CompletableFuture<?>[] indexingFutures;

		public void startIndexingList(PojoMassIndexingSessionContext sessionContext, PojoIndexer indexer,
				List<?> entities)
				throws InterruptedException {
			this.sessionContext = sessionContext;
			this.entities = entities;
			getNotifier().reportEntitiesLoaded( entities.size() );
			this.indexingFutures = new CompletableFuture<?>[entities.size()];

			for ( int i = 0; i < entities.size(); i++ ) {
				Object entity = entities.get( i );
				indexingFutures[i] = startIndexing( sessionContext, indexer, entity );
			}
		}

		private void waitForIndexingEndAndReport() throws InterruptedException {
			if ( indexingFutures == null ) {
				// No indexing in progress
				return;
			}

			Futures.unwrappedExceptionGet(
					CompletableFuture.allOf( indexingFutures )
							// We handle exceptions on a per-entity basis below, so we ignore them here.
							.exceptionally( exception -> null )
			);

			int successfulEntities = 0;
			for ( int i = 0; i < entities.size(); i++ ) {
				CompletableFuture<?> future = indexingFutures[i];

				if ( future.isCompletedExceptionally() ) {
					Object entity = entities.get( i );
					getNotifier().reportEntityIndexingFailure(
							// We don't try to detect the exact entity type here,
							// because that could fail if the type is not indexed
							// (which should not happen, but well... failures should not happen to begin with).
							typeGroup, sessionContext, entity,
							Throwables.expectException( Futures.getThrowableNow( future ) )
					);
				}
				else {
					++successfulEntities;
				}
			}

			getNotifier().reportDocumentsAdded( successfulEntities );
			typeGroupMonitor.documentsIndexed( successfulEntities );

			this.sessionContext = null;
			this.entities = null;
			this.indexingFutures = null;
		}

		private CompletableFuture<?> startIndexing(PojoMassIndexingSessionContext sessionContext,
				PojoIndexer indexer, Object entity)
				throws InterruptedException {
			// abort if the thread has been interrupted while not in wait(), I/O or similar which themselves would have
			// raised the InterruptedException
			if ( Thread.currentThread().isInterrupted() ) {
				throw new InterruptedException();
			}

			CompletableFuture<?> future;
			try {
				PojoRawTypeIdentifier<?> typeIdentifier = detectTypeIdentifier( sessionContext, entity );
				future = indexer.add( typeIdentifier, null, null, entity,
						// Commit and refresh are handled globally after all documents are indexed.
						DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE, OperationSubmitter.blocking()
				);
			}
			catch (RuntimeException e) {
				future = new CompletableFuture<>();
				future.completeExceptionally( e );
				return future;
			}

			// Only if the above succeeded
			getNotifier().reportDocumentBuilt();

			return future;
		}

		private PojoRawTypeIdentifier<?> detectTypeIdentifier(PojoMassIndexingSessionContext sessionContext,
				Object entity) {
			return sessionContext.runtimeIntrospector().detectEntityType( entity );
		}
	}

	private static final class EntityLoadingContextImpl implements MassIndexingEnvironment.EntityLoadingContext {
	}
}
