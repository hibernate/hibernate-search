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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.LongAdder;
import javax.persistence.LockModeType;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.SingularAttribute;
import javax.transaction.TransactionManager;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.query.Query;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexer;
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

	private final ProducerConsumerQueue<List<I>> source;
	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final CacheMode cacheMode;
	private final Class<E> type;
	private final MassIndexingMonitor monitor;
	private final FailureHandler failureHandler;
	private final SingularAttribute<? super E, I> idAttributeOfIndexedType;
	private final CountDownLatch producerEndSignal;
	private final Integer transactionTimeout;
	private final String tenantId;

	/**
	 * The JTA transaction manager or {@code null} if not in a JTA environment
	 */
	private final TransactionManager transactionManager;

	IdentifierConsumerDocumentProducer(
			ProducerConsumerQueue<List<I>> fromIdentifierListToEntities,
			MassIndexingMonitor monitor, FailureHandler failureHandler,
			HibernateOrmMassIndexingMappingContext mappingContext,
			CountDownLatch producerEndSignal, CacheMode cacheMode,
			Class<E> indexedType, SingularAttribute<? super E, I> idAttributeOfIndexedType, Integer transactionTimeout,
			String tenantId) {
		this.source = fromIdentifierListToEntities;
		this.mappingContext = mappingContext;
		this.cacheMode = cacheMode;
		this.type = indexedType;
		this.monitor = monitor;
		this.failureHandler = failureHandler;
		this.idAttributeOfIndexedType = idAttributeOfIndexedType;
		this.producerEndSignal = producerEndSignal;
		this.transactionTimeout = transactionTimeout;
		this.tenantId = tenantId;
		this.transactionManager = mappingContext.getSessionFactory()
				.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();

		log.trace( "created" );
	}

	@Override
	public void run() {
		log.trace( "started" );
		SessionImplementor session = (SessionImplementor) mappingContext.getSessionFactory()
				.withOptions()
				.tenantIdentifier( tenantId )
				.openSession();
		session.setHibernateFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		session.setDefaultReadOnly( true );
		try {
			loadAllFromQueue( session );
		}
		catch (Exception exception) {
			String logMessage = log.massIndexerExceptionWhileTransformingIds();

			failureHandler.handleException( logMessage, exception );
		}
		finally {
			producerEndSignal.countDown();
			session.close();
		}
		log.trace( "finished" );
	}

	private void loadAllFromQueue(SessionImplementor session) throws Exception {
		// The search session will be closed automatically with the ORM session
		PojoIndexer indexer = mappingContext.createIndexer(
				session, DocumentCommitStrategy.NONE
		);
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
	private void loadList(List<I> listIds, SessionImplementor session, PojoIndexer indexer) throws Exception {
		try {
			beginTransaction( session );

			CriteriaBuilder criteriaBuilder = session.getCriteriaBuilder();
			CriteriaQuery<E> criteriaQuery = criteriaBuilder.createQuery( type );
			Root<E> root = criteriaQuery.from( type );
			criteriaQuery.select( root );
			criteriaQuery.where( root.get( idAttributeOfIndexedType ).in( listIds ) );

			Query<E> query = session.createQuery( criteriaQuery )
					.setCacheMode( cacheMode )
					.setLockMode( LockModeType.NONE )
					.setCacheable( false )
					.setHibernateFlushMode( FlushMode.MANUAL )
					.setFetchSize( listIds.size() );

			indexAllQueue( indexer, query.getResultList() );
			session.clear();
		}
		finally {
			// it's read-only, so no need to commit
			rollbackTransaction( session );
		}
	}

	private void beginTransaction(Session session) throws Exception {
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

	private void indexAllQueue(PojoIndexer indexer, List<E> entities) throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		monitor.entitiesLoaded( entities.size() );
		CompletableFuture<?>[] futures = new CompletableFuture<?>[entities.size()];

		LongAdder failedEntitiesAdder = new LongAdder();
		for ( int i = 0; i < entities.size(); i++ ) {
			final E entity = entities.get( i );
			futures[i] = index( indexer, entity );
			futures[i].exceptionally( exception -> {
				failedEntitiesAdder.increment();
				handleException( entity, exception );
				return null;
			} );
		}

		CompletableFuture.allOf( futures )
				// We handle exceptions on a per-entity basis, so we ignore them here.
				.exceptionally( exception -> null )
				.join();

		monitor.documentsAdded( entities.size() - failedEntitiesAdder.longValue() );
	}

	private CompletableFuture<?> index(PojoIndexer indexer, E entity) throws InterruptedException {
		// abort if the thread has been interrupted while not in wait(), I/O or similar which themselves would have
		// raised the InterruptedException
		if ( Thread.currentThread().isInterrupted() ) {
			throw new InterruptedException();
		}

		CompletableFuture<?> future;
		try {
			future = indexer.add( entity );
		}
		catch (RuntimeException e) {
			future = new CompletableFuture<>();
			future.completeExceptionally( e );
			return future;
		}

		// Only if the above succeeded
		monitor.documentsBuilt( 1 );

		return future;
	}

	private void handleException(Object entity, Throwable e) {
		String errorMsg = log.massIndexerUnableToIndexInstance( entity.getClass().getName(), entity.toString() );

		failureHandler.handleException( errorMsg, e );
	}
}
