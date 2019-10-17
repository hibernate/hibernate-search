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
import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.common.impl.EntityReferenceImpl;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
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

	private final ProducerConsumerQueue<List<I>> source;
	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final CacheMode cacheMode;
	private final Class<E> type;
	private final String entityName;
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
			Class<E> indexedType, String entityName, SingularAttribute<? super E, I> idAttributeOfIndexedType,
			Integer transactionTimeout,
			String tenantId) {
		this.source = fromIdentifierListToEntities;
		this.mappingContext = mappingContext;
		this.cacheMode = cacheMode;
		this.type = indexedType;
		this.entityName = entityName;
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
			FailureContext.Builder failureContextBuilder = FailureContext.builder();
			failureContextBuilder.throwable( exception );
			failureContextBuilder.failingOperation( log.massIndexingLoadingAndExtractingEntityData( entityName ) );
			failureHandler.handle( failureContextBuilder.build() );
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

			indexAllQueue( session, indexer, query.getResultList() );
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

	private void indexAllQueue(Session session, PojoIndexer indexer, List<E> entities) throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		monitor.entitiesLoaded( entities.size() );
		CompletableFuture<?>[] indexingFutures = new CompletableFuture<?>[entities.size()];

		for ( int i = 0; i < entities.size(); i++ ) {
			final E entity = entities.get( i );
			indexingFutures[i] = index( indexer, entity );
		}

		Futures.unwrappedExceptionJoin(
				CompletableFuture.allOf( indexingFutures )
						// We handle exceptions on a per-entity basis below, so we ignore them here.
						.exceptionally( exception -> null )
		);

		int successfulEntities = 0;
		for ( int i = 0; i < entities.size(); i++ ) {
			CompletableFuture<?> future = indexingFutures[i];

			if ( future.isCompletedExceptionally() ) {
				handleIndexingFailure( session, entities.get( i ), Futures.getThrowableNow( future ) );
			}
			else {
				++successfulEntities;
			}
		}

		monitor.documentsAdded( successfulEntities );
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

	private void handleIndexingFailure(Session session, Object entity, Throwable throwable) {
		EntityIndexingFailureContext.Builder contextBuilder = EntityIndexingFailureContext.builder();
		contextBuilder.throwable( throwable );
		// Add minimal information here, but information we're sure we can get
		contextBuilder.failingOperation( log.massIndexerIndexingInstance( entityName ) );
		// Add more information here, but information that may not be available if the session completely broke down
		// (we're being extra careful here because we don't want to throw an exception while handling and exception)
		EntityReference entityReference = extractReferenceOrSuppress( session, entity, throwable );
		if ( entityReference != null ) {
			contextBuilder.entityReference( entityReference );
		}
		failureHandler.handle( contextBuilder.build() );
	}

	private EntityReference extractReferenceOrSuppress(Session session, Object entity, Throwable throwable) {
		try {
			return new EntityReferenceImpl( type, entityName, session.getIdentifier( entity ) );
		}
		catch (RuntimeException e) {
			// We failed to extract a reference.
			// Let's just give up and suppress the exception.
			throwable.addSuppressed( e );
			return null;
		}
	}
}
