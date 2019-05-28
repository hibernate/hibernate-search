/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import javax.transaction.TransactionManager;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.transaction.jta.platform.spi.JtaPlatform;
import org.hibernate.internal.CriteriaImpl;
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.mapper.orm.session.spi.SearchSessionImplementor;
import org.hibernate.search.mapper.pojo.work.spi.PojoSessionWorkExecutor;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * This {@code SessionAwareRunnable} is consuming entity identifiers and
 * producing corresponding {@code AddLuceneWork} instances being forwarded
 * to the index writing backend.
 * It will finish when the queue it is consuming from will
 * signal there are no more identifiers.
 *
 * @author Sanne Grinovero
 */
public class IdentifierConsumerDocumentProducer implements Runnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ProducerConsumerQueue<List<Serializable>> source;
	private final SessionFactory sessionFactory;
	private final CacheMode cacheMode;
	private final Class<?> type;
	private final MassIndexingMonitor monitor;
	private final String idName;
	private final CountDownLatch producerEndSignal;
	private final Integer transactionTimeout;
	private final String tenantId;
	private final HibernateOrmMapping mapping;

	/**
	 * The JTA transaction manager or {@code null} if not in a JTA environment
	 */
	private final TransactionManager transactionManager;

	public IdentifierConsumerDocumentProducer(
			ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities, MassIndexingMonitor monitor,
			SessionFactory sessionFactory, CountDownLatch producerEndSignal, CacheMode cacheMode,
			Class<?> indexedType, String idName, Integer transactionTimeout,
			String tenantId, HibernateOrmMapping mapping) {
		this.source = fromIdentifierListToEntities;
		this.sessionFactory = sessionFactory;
		this.cacheMode = cacheMode;
		this.type = indexedType;
		this.monitor = monitor;
		this.idName = idName;
		this.producerEndSignal = producerEndSignal;
		this.transactionTimeout = transactionTimeout;
		this.tenantId = tenantId;
		this.mapping = mapping;
		this.transactionManager = ( (SessionFactoryImplementor) sessionFactory )
				.getServiceRegistry()
				.getService( JtaPlatform.class )
				.retrieveTransactionManager();

		log.trace( "created" );
	}

	@Override
	public void run() {
		log.trace( "started" );
		SessionImplementor session = (SessionImplementor) sessionFactory
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

			// TODO HSEARCH-3110 use an errorHandler instance
			// errorHandler.handleException( logMessage, exception );
			// temporary re-throw the exception as a Runtime
			throw new RuntimeException( logMessage, exception );
		}
		finally {
			producerEndSignal.countDown();
			session.close();
		}
		log.trace( "finished" );
	}

	private void loadAllFromQueue(SessionImplementor session) throws Exception {
		try ( SearchSessionImplementor searchSession = mapping.createSession( session ) ) {
			PojoSessionWorkExecutor workExecutor =
					searchSession.createSessionWorkExecutor( DocumentCommitStrategy.NONE );
			List<Serializable> idList;
			do {
				idList = source.take();
				if ( idList != null ) {
					log.tracef( "received list of ids %s", idList );
					loadList( idList, session, workExecutor );
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
	 * @param workExecutor the work executor to be used
	 */
	private void loadList(List<Serializable> listIds, SessionImplementor session, PojoSessionWorkExecutor workExecutor) throws Exception {
		try {
			beginTransaction( session );

			Criteria criteria = new CriteriaImpl( type.getName(), session )
					.setCacheMode( cacheMode )
					.setLockMode( LockMode.NONE )
					.setCacheable( false )
					.setFlushMode( FlushMode.MANUAL )
					.setFetchSize( listIds.size() )
					.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
					.add( Restrictions.in( idName, listIds ) );
			List<?> list = criteria.list();

			indexAllQueue( workExecutor, list );
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

	private void indexAllQueue(PojoSessionWorkExecutor workExecutor, List<?> entities) throws InterruptedException {
		if ( entities == null || entities.isEmpty() ) {
			return;
		}

		monitor.entitiesLoaded( entities.size() );
		CompletableFuture<?>[] futures = new CompletableFuture<?>[entities.size()];

		for ( int i = 0; i < entities.size(); i++ ) {
			final Object entity = entities.get( i );
			futures[i] = index( workExecutor, entity );
			futures[i].exceptionally( exception -> {
				handleException( entity, exception );
				return null;
			} );
		}
		// handle exceptions on a per-work basis
		CompletableFuture.allOf( futures ).exceptionally( exception -> null ).join();

		monitor.documentsAdded( entities.size() );
	}

	private CompletableFuture<?> index(PojoSessionWorkExecutor workExecutor, Object entity) throws InterruptedException {
		// abort if the thread has been interrupted while not in wait(), I/O or similar which themselves would have
		// raised the InterruptedException
		if ( Thread.currentThread().isInterrupted() ) {
			throw new InterruptedException();
		}

		CompletableFuture<?> future = Futures.create( () -> workExecutor.add( entity )
				.exceptionally( exception -> {
					handleException( entity, exception );
					return null;
				} ) );

		monitor.documentsBuilt( 1 );
		return future;
	}

	private void handleException(Object entity, Throwable e) {
		String errorMsg = log.massIndexerUnableToIndexInstance( entity.getClass().getName(), entity.toString() );

		// TODO HSEARCH-3110 implements exception handler
		// errorHandler.handleException( errorMsg, e );
		// temporary re-throw the exception
		throw new RuntimeException( errorMsg, e );
	}
}
