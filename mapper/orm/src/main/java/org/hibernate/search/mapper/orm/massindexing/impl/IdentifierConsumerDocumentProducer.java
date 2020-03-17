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
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.query.Query;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
import org.hibernate.search.util.common.impl.Futures;
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

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final String tenantId;
	private final MassIndexingNotifier notifier;

	private final HibernateOrmMassIndexingIndexedTypeContext<E> type;
	private final SingularAttribute<? super E, I> idAttributeOfType;

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
			HibernateOrmMassIndexingIndexedTypeContext<E> type, SingularAttribute<? super E, I> idAttributeOfType,
			ProducerConsumerQueue<List<I>> fromIdentifierListToEntities,
			CacheMode cacheMode,
			Integer transactionTimeout
			) {
		this.mappingContext = mappingContext;
		this.tenantId = tenantId;
		this.notifier = notifier;
		this.source = fromIdentifierListToEntities;
		this.cacheMode = cacheMode;
		this.type = type;
		this.idAttributeOfType = idAttributeOfType;
		this.transactionTimeout = transactionTimeout;
		this.transactionManager = mappingContext.getSessionFactory()
				.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();

		log.trace( "created" );
	}

	@Override
	public void run() {
		log.trace( "started" );
		try ( SessionImplementor session = (SessionImplementor) mappingContext.getSessionFactory()
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession() ) {
			session.setHibernateFlushMode( FlushMode.MANUAL );
			session.setCacheMode( cacheMode );
			session.setDefaultReadOnly( true );
			loadAllFromQueue( session );
		}
		catch (Exception exception) {
			notifier.notifyRunnableFailure(
					exception,
					log.massIndexingLoadingAndExtractingEntityData( type.getJpaEntityName() )
			);
		}
		log.trace( "finished" );
	}

	private void loadAllFromQueue(SessionImplementor session) throws SystemException, NotSupportedException {
		// The search session will be closed automatically with the ORM session
		PojoIndexer indexer = mappingContext.createIndexer( session );
		try {
			List<I> idList;
			do {
				idList = source.take();
				if ( idList != null ) {
					log.tracef( "received list of ids %s", idList );
					loadList( idList, session, indexer );
				}
			}
			while ( idList != null );
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Loads a list of entities of defined type using their identifiers.
	 * entities are then transformed into Lucene Documents
	 * and forwarded to the indexing backend.
	 *
	 * @param listIds the list of entity identifiers (of type
	 * @param session the session to be used
	 * @param indexer the indexer to be used
	 */
	private void loadList(List<I> listIds, SessionImplementor session, PojoIndexer indexer)
			throws InterruptedException, NotSupportedException, SystemException {
		try {
			beginTransaction( session );

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( type.getEntityTypeDescriptor().getJavaType() );
			Root<E> root = criteriaQuery.from( type.getEntityTypeDescriptor() );
			criteriaQuery.select( root );
			criteriaQuery.where( root.get( idAttributeOfType ).in( listIds ) );

			Query<E> query = session.createQuery( criteriaQuery )
					.setCacheMode( cacheMode )
					.setLockMode( LockModeType.NONE )
					.setCacheable( false )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( listIds.size() );

			indexAllQueue( session, indexer, query.getResultList() );
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

	private void indexAllQueue(Session session, PojoIndexer indexer, List<E> entities) throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		notifier.notifyEntitiesLoaded( entities.size() );
		CompletableFuture<?>[] indexingFutures = new CompletableFuture<?>[entities.size()];

		for ( int i = 0; i < entities.size(); i++ ) {
			final E entity = entities.get( i );
			indexingFutures[i] = index( indexer, entity );
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
				notifier.notifyEntityIndexingFailure(
						type,
						session, entities.get( i ),
						Futures.getThrowableNow( future )
				);
			}
			else {
				++successfulEntities;
			}
		}

		notifier.notifyDocumentsAdded( successfulEntities );
	}

	private CompletableFuture<?> index(PojoIndexer indexer, E entity) throws InterruptedException {
		// abort if the thread has been interrupted while not in wait(), I/O or similar which themselves would have
		// raised the InterruptedException
		if ( Thread.currentThread().isInterrupted() ) {
			throw new InterruptedException();
		}

		CompletableFuture<?> future;
		try {
			future = indexer.add( type.getTypeIdentifier(), null, entity );
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

}
