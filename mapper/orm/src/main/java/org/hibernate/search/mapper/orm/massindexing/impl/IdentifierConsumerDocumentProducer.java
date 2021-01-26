/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.persistence.LockModeType;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.scope.impl.HibernateOrmScopeSessionContext;
import org.hibernate.search.mapper.pojo.model.spi.PojoRawTypeIdentifier;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.Throwables;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * This {@code SessionAwareRunnable} is consuming entity identifiers and
 * producing corresponding {@code AddLuceneWork} instances being forwarded
 * to the index writing backend.
 * It will finish when the queue it is consuming from will
 * signal there are no more identifiers.
 *
 * @param <E> The entity type
 * @param <I> The identifier type
 *
 * @author Sanne Grinovero
 */
public class IdentifierConsumerDocumentProducer<E, I> implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private static final String ID_PARAMETER_NAME = "ids";

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final String tenantId;
	private final MassIndexingNotifier notifier;

	private final MassIndexingIndexedTypeGroup<E, I> typeGroup;
	private final MassIndexingTypeGroupLoader<? super E, I> typeGroupLoader;

	private final ProducerConsumerQueue<List<I>> source;
	private final CacheMode cacheMode;
	private final Integer transactionTimeout;

	/**
	 * The JTA transaction manager or {@code null} if not in a JTA environment
	 */
	private final TransactionManager transactionManager;

	IdentifierConsumerDocumentProducer(
			HibernateOrmMassIndexingMappingContext mappingContext, String tenantId,
			MassIndexingNotifier notifier,
			MassIndexingIndexedTypeGroup<E, I> typeGroup,
			MassIndexingTypeGroupLoader<? super E, I> typeGroupLoader,
			ProducerConsumerQueue<List<I>> fromIdentifierListToEntities,
			CacheMode cacheMode,
			Integer transactionTimeout
			) {
		this.mappingContext = mappingContext;
		this.tenantId = tenantId;
		this.notifier = notifier;
		this.typeGroup = typeGroup;
		this.typeGroupLoader = typeGroupLoader;
		this.source = fromIdentifierListToEntities;
		this.cacheMode = cacheMode;
		this.transactionTimeout = transactionTimeout;
		this.transactionManager = mappingContext.sessionFactory()
				.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();

		log.trace( "created" );
	}

	@Override
	public void run() {
		log.trace( "started" );
		try ( SessionImplementor session = (SessionImplementor) mappingContext.sessionFactory()
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession() ) {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.setCacheMode( cacheMode );
			session.setDefaultReadOnly( true );
			loadAndIndexAllFromQueue( session );
		}
		catch (Exception exception) {
			notifier.notifyRunnableFailure(
					exception,
					log.massIndexingLoadingAndExtractingEntityData( typeGroup.includedEntityNames() )
			);
		}
		log.trace( "finished" );
	}

	private void loadAndIndexAllFromQueue(SessionImplementor session) throws SystemException, NotSupportedException {
		HibernateOrmScopeSessionContext sessionContext = mappingContext.sessionContext( session );
		PojoIndexer indexer = sessionContext.createIndexer();
		try {
			List<I> idList;
			do {
				idList = source.take();
				if ( idList != null ) {
					log.tracef( "received list of ids %s", idList );
					loadAndIndexList( idList, sessionContext, indexer );
				}
			}
			while ( idList != null );
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
	}

	private void loadAndIndexList(List<I> listIds, HibernateOrmMassIndexingSessionContext sessionContext,
			PojoIndexer indexer)
			throws InterruptedException, NotSupportedException, SystemException {
		SessionImplementor session = sessionContext.session();
		try {
			beginTransaction( session );

			Query<? super E> query = typeGroupLoader.createLoadingQuery( session, ID_PARAMETER_NAME )
					.setParameter( ID_PARAMETER_NAME, listIds )
					.setCacheMode( cacheMode )
					.setLockMode( LockModeType.NONE )
					.setCacheable( false )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( listIds.size() );

			indexList( sessionContext, indexer, query.getResultList() );
			session.clear();
		}
		finally {
			// it's read-only, so no need to commit
			rollbackTransaction( session );
		}
	}

	private void beginTransaction(Session session) throws SystemException, NotSupportedException {
		if ( transactionManager != null ) {
			if ( transactionTimeout != null ) {
				transactionManager.setTransactionTimeout( transactionTimeout );
			}

			transactionManager.begin();
		}
		else {
			session.beginTransaction();
		}
	}

	private void rollbackTransaction(SessionImplementor session) {
		try {
			if ( transactionManager != null ) {
				transactionManager.rollback();
			}
			else {
				session.accessTransaction().rollback();
			}
		}
		catch (Exception e) {
			log.errorRollingBackTransaction( e.getMessage(), e );
		}
	}

	private void indexList(HibernateOrmMassIndexingSessionContext sessionContext, PojoIndexer indexer,
			List<? super E> entities)
			throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		notifier.notifyEntitiesLoaded( entities.size() );
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

	private CompletableFuture<?> index(HibernateOrmMassIndexingSessionContext sessionContext,
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

	private PojoRawTypeIdentifier<?> detectTypeIdentifier(HibernateOrmMassIndexingSessionContext sessionContext,
			Object entity) {
		return sessionContext.runtimeIntrospector().detectEntityType( entity );
	}

}
