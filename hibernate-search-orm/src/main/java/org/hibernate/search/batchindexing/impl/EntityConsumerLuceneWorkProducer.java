    /*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.batchindexing.impl;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.bridge.spi.ConversionContext;
import org.hibernate.search.bridge.util.impl.ContextualExceptionBridgeHelper;
import org.hibernate.search.engine.impl.HibernateSessionLoadingInitializer;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.spi.EntityIndexBinder;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.InstanceInitializer;
import org.hibernate.search.util.impl.HibernateHelper;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Component of batch-indexing pipeline, using chained producer-consumers.
 * This Runnable will consume entities taken one-by-one from the queue
 * and produce for each entity an AddLuceneWork to the output queue.
 *
 * @author Sanne Grinovero
 */
public class EntityConsumerLuceneWorkProducer implements SessionAwareRunnable {

	private static final Log log = LoggerFactory.make();

	private final ProducerConsumerQueue<List<?>> source;
	private final SessionFactory sessionFactory;
	private final Map<Class<?>, EntityIndexBinder> entityIndexBinders;
	private final MassIndexerProgressMonitor monitor;
	private final CacheMode cacheMode;
	private final CountDownLatch producerEndSignal;
	private final BatchBackend backend;
	private final ErrorHandler errorHandler;

	public EntityConsumerLuceneWorkProducer(
			ProducerConsumerQueue<List<?>> entitySource,
			MassIndexerProgressMonitor monitor,
			SessionFactory sessionFactory,
			CountDownLatch producerEndSignal,
			SearchFactoryImplementor searchFactory, CacheMode cacheMode,
			BatchBackend backend, ErrorHandler errorHandler) {
		this.source = entitySource;
		this.monitor = monitor;
		this.sessionFactory = sessionFactory;
		this.producerEndSignal = producerEndSignal;
		this.cacheMode = cacheMode;
		this.backend = backend;
		this.errorHandler = errorHandler;
		this.entityIndexBinders = searchFactory.getIndexBindingForEntity();
	}

	public void run(Session upperSession) {
		Session session = upperSession;
		if ( upperSession == null ) {
			session = sessionFactory.openSession();
		}
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		session.setDefaultReadOnly( true );
		try {
			Transaction transaction = Helper.getTransactionAndMarkForJoin( session );
			transaction.begin();
			indexAllQueue( session );
			transaction.commit();
		}
		catch (Throwable e) {
			errorHandler.handleException( log.massIndexerUnexpectedErrorMessage() , e );
		}
		finally {
			producerEndSignal.countDown();
			if ( upperSession == null ) {
				session.close();
			}
		}
		log.debug( "finished" );
	}

	private void indexAllQueue(Session session) {
		final InstanceInitializer sessionInitializer = new HibernateSessionLoadingInitializer(
				(SessionImplementor) session );
		try {
			ConversionContext contextualBridge = new ContextualExceptionBridgeHelper();
			while ( true ) {
				List<?> takeList = source.take();
				if ( takeList == null ) {
					break;
				}
				else {
					log.tracef( "received a list of objects to index: %s", takeList );
					for ( Object take : takeList ) {
						//trick to attach the objects to session:
						session.buildLockRequest( LockOptions.NONE ).lock( take );
						index( take, session, sessionInitializer, contextualBridge );
						monitor.documentsBuilt( 1 );
						session.clear();
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
		EntityIndexBinder entityIndexBinding = entityIndexBinders.get( clazz );
		if ( entityIndexBinding == null ) {
			// it might be possible to receive not-indexes subclasses of the currently indexed type;
			// being not-indexed, we skip them.
			// FIXME for improved performance: avoid loading them in an early phase.
			return;
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
