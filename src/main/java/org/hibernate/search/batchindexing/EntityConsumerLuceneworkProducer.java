/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
package org.hibernate.search.batchindexing;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.hibernate.CacheMode;
import org.hibernate.FlushMode;
import org.hibernate.Hibernate;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.bridge.TwoWayFieldBridge;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Component of batch-indexing pipeline, using chained producer-consumers.
 * This Runnable will consume entities taken one-by-one from the queue
 * and produce for each entity an AddLuceneWork to the output queue.
 * 
 * @author Sanne Grinovero
 */
public class EntityConsumerLuceneworkProducer implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final ProducerConsumerQueue<Object> source;
	private final SessionFactory sessionFactory;
	private final Map<Class<?>, DocumentBuilderIndexedEntity<?>> documentBuilders;
	private final MassIndexerProgressMonitor monitor;
	
	private static final int CLEAR_PERIOD = 50;
	private final CacheMode cacheMode;

	private final CountDownLatch producerEndSignal;

	private final BatchBackend backend;
	
	public EntityConsumerLuceneworkProducer(
			ProducerConsumerQueue<Object> entitySource,
			MassIndexerProgressMonitor monitor,
			SessionFactory sessionFactory,
			CountDownLatch producerEndSignal,
			SearchFactoryImplementor searchFactory, CacheMode cacheMode, BatchBackend backend) {
		this.source = entitySource;
		this.monitor = monitor;
		this.sessionFactory = sessionFactory;
		this.producerEndSignal = producerEndSignal;
		this.cacheMode = cacheMode;
		this.backend = backend;
		this.documentBuilders = searchFactory.getDocumentBuildersIndexedEntities();
	}

	public void run() {
		Session session = sessionFactory.openSession();
		session.setFlushMode( FlushMode.MANUAL );
		session.setCacheMode( cacheMode );
		try {
			Transaction transaction = session.beginTransaction();
			indexAllQueue( session );
			transaction.commit();
		}
		finally {
			producerEndSignal.countDown();
			session.close();
		}
		log.debug( "finished" );
	}

	private void indexAllQueue(Session session) {
		try {
			for ( int cycle=0; true; cycle++ ) {
				Object take = source.take();
				if ( take == null ) {
					break;
				}
				else {
					log.trace( "received an object {}", take );
					//trick to attach the objects to session:
					session.lock( take, LockMode.NONE );
					index( take, session );
					monitor.documentsBuilt( 1 );
					session.evict( take );
					if ( cycle == CLEAR_PERIOD ) {
						cycle = 0;
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
	private void index( Object entity, Session session ) throws InterruptedException {
		Serializable id = session.getIdentifier( entity );
		Class clazz = Hibernate.getClass( entity );
		DocumentBuilderIndexedEntity docBuilder = documentBuilders.get( clazz );
		TwoWayFieldBridge idBridge = docBuilder.getIdBridge();
		String idInString = idBridge.objectToString( id );
		//depending on the complexity of the object graph going to be indexed it's possible
		//that we hit the database several times during work construction.
		AddLuceneWork addWork = docBuilder.createAddWork( clazz, entity, id, idInString, true );
		backend.enqueueAsyncWork( addWork );
	}
	
}
