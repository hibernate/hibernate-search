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
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.pojo.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.mapper.pojo.massindexing.spi.MassIndexingSessionContext;
import org.hibernate.search.mapper.pojo.intercepting.LoadingInvocationContext;
import org.hibernate.search.mapper.pojo.intercepting.spi.PojoInterceptingInvoker;
import org.hibernate.search.mapper.pojo.intercepting.spi.PojoInterceptingNextInvoker;
import org.hibernate.search.mapper.pojo.loading.EntityIdentifierScroll;
import org.hibernate.search.mapper.pojo.loading.EntityLoader;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;

class PojoMassIndexingTypeProcessor {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoMassIndexingNotifier notifier;
	private final PojoMassIndexingIndexedTypeGroup<?> typeGroup;

	private final PojoProducerConsumerQueue<List<?>> primaryKeyStream;

	public PojoMassIndexingTypeProcessor(
			PojoMassIndexingNotifier notifier,
			PojoMassIndexingIndexedTypeGroup<?> typeGroup) {
		this.notifier = notifier;
		this.typeGroup = typeGroup;

		//pipelining queues:
		this.primaryKeyStream = new PojoProducerConsumerQueue<>( 1 );
	}

	public PojoInterceptingInvoker identifierProducer() {
		return (ictx, invoker) -> {
			log.trace( "started" );
			try {
				invoker.invoke( () -> this.loadAllIdentifiers( ictx ) );
			}
			catch (InterruptedException e) {
				// just quit
				Thread.currentThread().interrupt();
			}
			catch (Exception exception) {
				notifier.notifyRunnableFailure( exception, log.massIndexerFetchingIds( typeGroup.notifiedGroupName() ) );
			}
			finally {
				primaryKeyStream.producerStopping();
			}
			log.trace( "finished" );
		};
	}

	public PojoInterceptingInvoker documentProducer() {
		return (ictx, invoker) -> {
			log.trace( "started" );
			try {
				loadAndIndexAllFromQueue( ictx, invoker );
			}
			catch (Exception exception) {
				notifier.notifyRunnableFailure(
						exception,
						log.massIndexingLoadingAndExtractingEntityData( typeGroup.notifiedGroupName() )
				);
			}
			log.trace( "finished" );
		};
	}

	private void loadAllIdentifiers(LoadingInvocationContext<?> ictx) throws InterruptedException {
		MassIndexingSessionContext sessionContext = (MassIndexingSessionContext) ictx.context( MassIndexingSessionContext.class );
		try ( EntityIdentifierScroll identifierScroll = typeGroup
				.createIdentifierScroll( new PojoMassIndexingThreadContext( ictx, typeGroup ), sessionContext ) ) {

			long totalCount = identifierScroll.totalCount();
			notifier.notifyAddedTotalCount( totalCount );
			List<?> ids = identifierScroll.next();
			while ( ids != null && !ids.isEmpty() ) {
				List<?> idsList = new ArrayList<>( ids );
				primaryKeyStream.put( idsList );
				log.tracef( "produced a list of ids %s", idsList );
				ids = identifierScroll.next();
			}
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
	}

	private void loadAndIndexAllFromQueue(LoadingInvocationContext<?> ictx, PojoInterceptingNextInvoker invoker) throws Exception {
		MassIndexingSessionContext sessionContext = (MassIndexingSessionContext) ictx.context( MassIndexingSessionContext.class );
		PojoIndexer indexer = sessionContext.createIndexer();
		try {
			List<?> idList;
			do {
				idList = primaryKeyStream.take();
				if ( idList != null ) {
					log.tracef( "received list of ids %s", idList );
					loadAndIndexList( ictx, idList, sessionContext, indexer, invoker );
				}
			}
			while ( idList != null );
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
	}

	private void loadAndIndexList(LoadingInvocationContext<?> ictx, List<?> listIds,
			MassIndexingSessionContext sessionContext,
			PojoIndexer indexer,
			PojoInterceptingNextInvoker invoker)
			throws Exception {
		invoker.invoke( () -> {
			try ( EntityLoader<?> entityLoader = typeGroup
					.createLoader( new PojoMassIndexingThreadContext( ictx, typeGroup ), sessionContext ) ) {
				List<?> result = entityLoader.load( listIds );
				indexList( sessionContext, indexer, result );
			}
		} );
	}

	private void indexList(MassIndexingSessionContext sessionContext, PojoIndexer indexer,
			List<?> entities)
			throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		notifier.notifyEntitiesLoaded( entities.size() );
		CompletableFuture<?>[] indexingFutures = new CompletableFuture<?>[entities.size()];

		for ( int i = 0; i < entities.size(); i++ ) {
			Object entity = entities.get( i );
			if ( !typeGroup.includesInstance( sessionContext, entity ) ) {
				continue;
			}
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
				notifier.notifyEntityIndexingFailure(
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

		notifier.notifyDocumentsAdded( successfulEntities );
	}

	private CompletableFuture<?> index(MassIndexingSessionContext sessionContext,
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
		notifier.notifyDocumentBuilt();

		return future;
	}

	private PojoRawTypeIdentifier<?> detectTypeIdentifier(MassIndexingSessionContext sessionContext,
			Object entity) {
		return sessionContext.runtimeIntrospector().detectEntityType( entity );
	}

}
