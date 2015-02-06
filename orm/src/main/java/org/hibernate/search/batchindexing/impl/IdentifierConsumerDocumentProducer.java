/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.hibernate.CacheMode;
import org.hibernate.Criteria;
import org.hibernate.FlushMode;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.Restrictions;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.impl.HibernateSessionLoadingInitializer;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.hcore.util.impl.HibernateHelper;
import org.hibernate.search.indexes.interceptor.EntityIndexingInterceptor;
import org.hibernate.search.indexes.interceptor.IndexingOverride;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This {@code SessionAwareRunnable} is consuming entity identifiers and
 * producing corresponding {@code AddLuceneWork} instances being forwarded
 * to the index writing backend.
 * It will finish when the queue it is consuming from will
 * signal there are no more identifiers.
 *
 * @author Sanne Grinovero
 */
public class IdentifierConsumerDocumentProducer implements SessionAwareRunnable {

	private static final Log log = LoggerFactory.make();

	private final ProducerConsumerQueue<List<Serializable>> source;
	private final SessionFactory sessionFactory;
	private final CacheMode cacheMode;
	private final Class<?> type;
	private final MassIndexerProgressMonitor monitor;
	private final Map<Class<?>, EntityIndexBinding> entityIndexBindings;
	private final String idName;
	private final ErrorHandler errorHandler;
	private final BatchBackend backend;
	private final CountDownLatch producerEndSignal;
	private final String tenantId;

	public IdentifierConsumerDocumentProducer(
			ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities,
			MassIndexerProgressMonitor monitor,
			SessionFactory sessionFactory,
			CountDownLatch producerEndSignal,
			CacheMode cacheMode, Class<?> type,
			ExtendedSearchIntegrator searchFactory,
			String idName, BatchBackend backend, ErrorHandler errorHandler,
			String tenantId) {
		this.source = fromIdentifierListToEntities;
		this.monitor = monitor;
		this.sessionFactory = sessionFactory;
		this.cacheMode = cacheMode;
		this.type = type;
		this.idName = idName;
		this.backend = backend;
		this.errorHandler = errorHandler;
		this.producerEndSignal = producerEndSignal;
		this.entityIndexBindings = searchFactory.getIndexBindings();
		this.tenantId = tenantId;
		log.trace( "created" );
	}

	@Override
	public void run(Session upperSession) throws Exception {
		log.trace( "started" );
		Session session = upperSession;
		if ( upperSession == null ) {
			if ( tenantId == null ) {
				session = sessionFactory.openSession();
			}
			else {
				session = sessionFactory.withOptions().tenantIdentifier( tenantId ).openSession();
			}
		}
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		session.setDefaultReadOnly( true );
		try {
			Transaction transaction = session.getTransaction();
			transaction.begin();
			loadAllFromQueue( session );
			transaction.commit();
		}
		catch (Exception exception) {
			errorHandler.handleException( log.massIndexerExceptionWhileTransformingIds(), exception );
		}
		finally {
			producerEndSignal.countDown();
			if ( upperSession == null ) {
				session.close();
			}
		}
		log.trace( "finished" );
	}

	private void loadAllFromQueue(Session session) {
		final InstanceInitializer sessionInitializer = new HibernateSessionLoadingInitializer(
				(SessionImplementor) session
		);
		try {
			Object take;
			do {
				take = source.take();
				if ( take != null ) {
					@SuppressWarnings("unchecked")
					List<Serializable> idList = (List<Serializable>) take;
					log.tracef( "received list of ids %s", idList );
					loadList( idList, session, sessionInitializer );
				}
			}
			while ( take != null );
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Loads a list of entities of defined type using their identifiers.
	 * The loaded objects are then transformed into Lucene Documents
	 * and forwarded to the indexing backend.
	 *
	 * @param listIds the list of entity identifiers (of type
	 * @param session the session to be used
	 * @param sessionInitializer
	 *
	 * @throws InterruptedException
	 */
	private void loadList(List<Serializable> listIds, Session session, InstanceInitializer sessionInitializer) throws InterruptedException {
		Criteria criteria = session
				.createCriteria( type )
				.setCacheMode( cacheMode )
				.setLockMode( LockMode.NONE )
				.setCacheable( false )
				.setFlushMode( FlushMode.MANUAL )
				.setFetchSize( listIds.size() )
				.setResultTransformer( CriteriaSpecification.DISTINCT_ROOT_ENTITY )
				.add( Restrictions.in( idName, listIds ) );
		List<?> list = criteria.list();
		monitor.entitiesLoaded( list.size() );
		indexAllQueue( session, list, sessionInitializer );
		session.clear();
	}

	private void indexAllQueue(Session session, List<?> entities, InstanceInitializer sessionInitializer) {
		try {
			ConversionContext contextualBridge = new ContextualExceptionBridgeHelper();
				if ( entities == null || entities.isEmpty() ) {
					return;
				}
				else {
					log.tracef( "received a list of objects to index: %s", entities );
					for ( Object object : entities ) {
						try {
							index( object, session, sessionInitializer, contextualBridge );
							monitor.documentsBuilt( 1 );
						}
						catch (InterruptedException ie) {
							// rethrowing the interrupted exception
							throw ie;
						}
						catch (RuntimeException e) {
							String errorMsg = log.massIndexerUnableToIndexInstance(
									object.getClass().getName(),
									object.toString()
							);
							errorHandler.handleException( errorMsg, e );
						}
					}
				}
		}
		catch (InterruptedException e) {
			// just quit
			Thread.currentThread().interrupt();
		}
	}

	@SuppressWarnings("unchecked")
	private void index(Object entity, Session session, InstanceInitializer sessionInitializer, ConversionContext conversionContext)
			throws InterruptedException {
		Serializable id = session.getIdentifier( entity );
		Class<?> clazz = HibernateHelper.getClass( entity );
		EntityIndexBinding entityIndexBinding = entityIndexBindings.get( clazz );
		if ( entityIndexBinding == null ) {
			// it might be possible to receive not-indexes subclasses of the currently indexed type;
			// being not-indexed, we skip them.
			// FIXME for improved performance: avoid loading them in an early phase.
			return;
		}

		EntityIndexingInterceptor interceptor = entityIndexBinding.getEntityIndexingInterceptor();
		if ( interceptor != null ) {
			IndexingOverride onAdd = interceptor.onAdd( entity );
			switch ( onAdd ) {
				case REMOVE:
				case SKIP:
					return;
			}
			//default: continue indexing this instance
		}

		DocumentBuilderIndexedEntity docBuilder = entityIndexBinding.getDocumentBuilder();
		TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
		conversionContext.pushProperty( docBuilder.getIdKeywordName() );
		String idInString = null;
		try {
			idInString = conversionContext
					.setClass( clazz )
					.twoWayConversionContext( idBridge )
					.objectToString( id );
		}
		finally {
			conversionContext.popProperty();
		}
		//depending on the complexity of the object graph going to be indexed it's possible
		//that we hit the database several times during work construction.
		AddLuceneWork addWork = docBuilder.createAddWork(
				clazz,
				entity,
				id,
				idInString,
				sessionInitializer,
				conversionContext
		);
		backend.enqueueAsyncWork( addWork );
	}
}
