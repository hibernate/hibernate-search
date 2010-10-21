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
package org.hibernate.search.batchindexing;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 * 
 * @author Sanne Grinovero
 */
public class BatchIndexingWorkspace implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final SearchFactoryImplementor searchFactory;
	private final SessionFactory sessionFactory;
	
	//following order shows the 4 stages of an entity flowing to the index:
	private final ThreadPoolExecutor 		execIdentifiersLoader;
	private final ProducerConsumerQueue<List<Serializable>> 	fromIdentifierListToEntities;
	private final ThreadPoolExecutor 		execFirstLoader;
	private final ProducerConsumerQueue<List<?>> 	fromEntityToAddwork;
	private final ThreadPoolExecutor		execDocBuilding;
	
	private final int objectLoadingThreadNum;
	private final int luceneworkerBuildingThreadNum;
	private final Class<?> indexedType;
	
	// status control
	private final CountDownLatch producerEndSignal; //released when we stop adding Documents to Index 
	private final CountDownLatch endAllSignal; //released when we release all locks and IndexWriter
	
	// progress monitor
	private final MassIndexerProgressMonitor monitor;

	// loading options
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;

	private final BatchBackend backend;
	
	private final long objectsLimit;

	public BatchIndexingWorkspace(SearchFactoryImplementor searchFactoryImplementor, SessionFactory sessionFactory,
			Class<?> entityType,
			int objectLoadingThreads, int collectionLoadingThreads,
			CacheMode cacheMode, int objectLoadingBatchSize,
			CountDownLatch endAllSignal,
			MassIndexerProgressMonitor monitor, BatchBackend backend,
			long objectsLimit) {
		
		this.indexedType = entityType;
		this.searchFactory = searchFactoryImplementor;
		this.sessionFactory = sessionFactory;
		
		//thread pool sizing:
		this.objectLoadingThreadNum = objectLoadingThreads;
		this.luceneworkerBuildingThreadNum = collectionLoadingThreads;//collections are loaded as needed by building the document
		
		//loading options:
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;
		this.backend = backend;
		
		//executors: (quite expensive constructor)
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		this.execIdentifiersLoader = Executors.newFixedThreadPool( 1, "identifierloader" );
		this.execFirstLoader = Executors.newFixedThreadPool( objectLoadingThreadNum, "entityloader" );
		this.execDocBuilding = Executors.newFixedThreadPool( luceneworkerBuildingThreadNum, "collectionsloader" );
		
		//pipelining queues:
		this.fromIdentifierListToEntities = new ProducerConsumerQueue<List<Serializable>>( 1 );
		this.fromEntityToAddwork = new ProducerConsumerQueue<List<?>>( objectLoadingThreadNum );
		
		//end signal shared with other instances:
		this.endAllSignal = endAllSignal;
		this.producerEndSignal = new CountDownLatch( luceneworkerBuildingThreadNum );
		
		this.monitor = monitor;
		this.objectsLimit = objectsLimit;
	}

	public void run() {
		try {
			
			//first start the consumers, then the producers (reverse order):
			for ( int i=0; i < luceneworkerBuildingThreadNum; i++ ) {
			//from entity to LuceneWork:
				execDocBuilding.execute( new EntityConsumerLuceneworkProducer(
						fromEntityToAddwork, monitor,
						sessionFactory, producerEndSignal, searchFactory,
						cacheMode, backend) );
			}
			for ( int i=0; i < objectLoadingThreadNum; i++ ) {
			//from primary key to loaded entity:
				execFirstLoader.execute( new IdentifierConsumerEntityProducer(
						fromIdentifierListToEntities, fromEntityToAddwork, monitor,
						sessionFactory, cacheMode, indexedType) );
			}
			//from class definition to all primary keys:
			execIdentifiersLoader.execute( new IdentifierProducer(
					fromIdentifierListToEntities, sessionFactory,
					objectLoadingBatchSize, indexedType, monitor,
					objectsLimit ) );
			
			//shutdown all executors:
			execIdentifiersLoader.shutdown();
			execFirstLoader.shutdown();
			execDocBuilding.shutdown();
			try {
				producerEndSignal.await(); //await for all work being sent to the backend
				log.debug( "All work for type {} has been produced", indexedType.getName() );
			} catch (InterruptedException e) {
				//restore interruption signal:
				Thread.currentThread().interrupt();
				throw new SearchException( "Interrupted on batch Indexing; index will be left in unknown state!", e );
			}
		}
		finally {
			endAllSignal.countDown();
		}
	}

}
