/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntityLoader;
import org.hibernate.search.mapper.pojo.loading.spi.PojoMassEntitySink;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingEntityLoadingContext;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingLoadingStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.mapper.pojo.massindexing.spi.PojoMassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class PojoMassIndexingEntityLoadingRunnable<E, I>
		extends PojoMassIndexingFailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMassIndexingIndexedTypeGroup<E> typeGroup;
	private final PojoMassIndexingLoadingStrategy<E, I> loadingStrategy;
	private final PojoProducerConsumerQueue<List<I>> identifierQueue;

	protected PojoMassIndexingEntityLoadingRunnable(PojoMassIndexingNotifier notifier,
			PojoMassIndexingIndexedTypeGroup<E> typeGroup,
			PojoMassIndexingLoadingStrategy<E, I> loadingStrategy,
			PojoProducerConsumerQueue<List<I>> identifierQueue) {
		super( notifier );
		this.typeGroup = typeGroup;
		this.loadingStrategy = loadingStrategy;
		this.identifierQueue = identifierQueue;
	}

	@Override
	protected void runWithFailureHandler() throws InterruptedException {
		log.trace( "started" );
		LoadingContext context = new LoadingContext();
		try ( PojoMassEntityLoader<I> entityLoader = loadingStrategy.createEntityLoader( context ) ) {
			List<I> idList;
			do {
				idList = identifierQueue.take();
				if ( idList != null ) {
					log.tracef( "received list of ids %s", idList );
					// This will pass the loaded entities to the sink, which will trigger indexing for those entities.
					entityLoader.load( idList );
				}
			}
			while ( idList != null && !context.done );
		}
		log.trace( "finished" );
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
	protected String operationName() {
		return log.massIndexingLoadingAndExtractingEntityData( typeGroup.notifiedGroupName() );
	}

	private void indexList(PojoMassIndexingSessionContext sessionContext, PojoIndexer indexer,
			List<?> entities) throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		getNotifier().reportEntitiesLoaded( entities.size() );
		CompletableFuture<?>[] indexingFutures = new CompletableFuture<?>[entities.size()];

		for ( int i = 0; i < entities.size(); i++ ) {
			Object entity = entities.get( i );
			indexingFutures[i] = index( sessionContext, indexer, entity );
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
	}

	private CompletableFuture<?> index(PojoMassIndexingSessionContext sessionContext,
			PojoIndexer indexer, Object entity) throws InterruptedException {
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
					DocumentCommitStrategy.NONE, DocumentRefreshStrategy.NONE );
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

	private final class LoadingContext implements PojoMassIndexingEntityLoadingContext<E> {
		private boolean done = false;

		@Override
		public Set<PojoRawTypeIdentifier<? extends E>> includedTypes() {
			return Collections.unmodifiableSet( typeGroup.includedTypesIdentifiers() );
		}

		@Override
		public PojoMassEntitySink<E> createSink(PojoMassIndexingSessionContext sessionContext) {
			PojoIndexer indexer = sessionContext.createIndexer();
			return new PojoMassEntitySink<E>() {
				@Override
				public void accept(List<? extends E> batch) throws InterruptedException {
					indexList( sessionContext, indexer, batch );
				}
			};
		}
	}

}
