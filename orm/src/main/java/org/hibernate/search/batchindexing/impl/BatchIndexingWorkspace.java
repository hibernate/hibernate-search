/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010-2014, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.impl.batch.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 */
public class BatchIndexingWorkspace extends ErrorHandledRunnable {

	private static final Log log = LoggerFactory.make();

	private final SessionFactoryImplementor sessionFactory;

	private final ProducerConsumerQueue<List<Serializable>> fromIdentifierListToEntities;
	private final ProducerConsumerQueue<List<?>> fromEntityToAddwork;

	private final int objectLoadingThreadNum;
	private final int luceneWorkerBuildingThreadNum;
	private final Class<?> indexedType;
	private final String idNameOfIndexedType;

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

	private final int idFetchSize;

	public BatchIndexingWorkspace(SearchFactoryImplementor searchFactoryImplementor,
								SessionFactoryImplementor sessionFactory,
								Class<?> entityType,
								int objectLoadingThreads,
								int collectionLoadingThreads,
								CacheMode cacheMode,
								int objectLoadingBatchSize,
								CountDownLatch endAllSignal,
								MassIndexerProgressMonitor monitor,
								BatchBackend backend,
								long objectsLimit,
								int idFetchSize) {
		super( searchFactoryImplementor );
		this.indexedType = entityType;
		this.idFetchSize = idFetchSize;
		this.idNameOfIndexedType = searchFactoryImplementor.getIndexBinding( entityType )
				.getDocumentBuilder()
				.getIdentifierName();
		this.sessionFactory = sessionFactory;

		//thread pool sizing:
		this.objectLoadingThreadNum = objectLoadingThreads;
		this.luceneWorkerBuildingThreadNum = collectionLoadingThreads;//collections are loaded as needed by building the document

		//loading options:
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;
		this.backend = backend;

		//pipelining queues:
		this.fromIdentifierListToEntities = new ProducerConsumerQueue<List<Serializable>>( 1 );
		this.fromEntityToAddwork = new ProducerConsumerQueue<List<?>>( objectLoadingThreadNum );

		//end signal shared with other instances:
		this.endAllSignal = endAllSignal;
		this.producerEndSignal = new CountDownLatch( luceneWorkerBuildingThreadNum );

		this.monitor = monitor;
		this.objectsLimit = objectsLimit;
	}

	@Override
	public void runWithErrroHandler() {
		try {
			final ErrorHandler errorHandler = searchFactoryImplementor.getErrorHandler();
			final BatchTransactionalContext btctx = new BatchTransactionalContext( searchFactoryImplementor, sessionFactory, errorHandler );
			//first start the consumers, then the producers (reverse order):
			//from entity to LuceneWork:
			startTransformationToLuceneWork( btctx, errorHandler );
			//from primary key to loaded entity:
			startTransformationToEntities( btctx, errorHandler );
			//from class definition to all primary keys:
			startProducingPrimaryKeys( btctx, errorHandler );
			try {
				producerEndSignal.await(); //await for all work being sent to the backend
				log.debugf( "All work for type %s has been produced", indexedType.getName() );
			}
			catch (InterruptedException e) {
				//restore interruption signal:
				Thread.currentThread().interrupt();
				throw new SearchException( "Interrupted on batch Indexing; index will be left in unknown state!", e );
			}
		}
		finally {
			endAllSignal.countDown();
		}
	}

	private void startProducingPrimaryKeys(BatchTransactionalContext btctx, ErrorHandler errorHandler) {
		final Runnable primaryKeyOutputter = new OptionallyWrapInJTATransaction( btctx,
				new IdentifierProducer(
						fromIdentifierListToEntities, sessionFactory,
						objectLoadingBatchSize, indexedType, monitor,
						objectsLimit, errorHandler, idFetchSize
				));
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		final ThreadPoolExecutor execIdentifiersLoader = Executors.newFixedThreadPool( 1, "identifierloader" );
		try {
			execIdentifiersLoader.execute( primaryKeyOutputter );
		}
		finally {
			execIdentifiersLoader.shutdown();
		}
	}

	private void startTransformationToEntities(BatchTransactionalContext btctx, ErrorHandler errorHandler) {
		final Runnable entityOutputter = new OptionallyWrapInJTATransaction( btctx,
				new IdentifierConsumerEntityProducer(
						fromIdentifierListToEntities, fromEntityToAddwork, monitor,
						sessionFactory, cacheMode, indexedType, idNameOfIndexedType
				));
		final ThreadPoolExecutor execFirstLoader = Executors.newFixedThreadPool( objectLoadingThreadNum, "entityloader" );
		try {
			for ( int i = 0; i < objectLoadingThreadNum; i++ ) {
				execFirstLoader.execute( entityOutputter );
			}
		}
		finally {
			execFirstLoader.shutdown();
		}
	}

	private void startTransformationToLuceneWork(BatchTransactionalContext btctx, ErrorHandler errorHandler) {
		final Runnable luceneOutputter = new OptionallyWrapInJTATransaction( btctx,
				new EntityConsumerLuceneWorkProducer(
					fromEntityToAddwork, monitor,
					sessionFactory, producerEndSignal, searchFactoryImplementor,
					cacheMode, backend, errorHandler
					));
		final ThreadPoolExecutor execDocBuilding = Executors.newFixedThreadPool( luceneWorkerBuildingThreadNum, "collectionsloader" );
		try {
			for ( int i = 0; i < luceneWorkerBuildingThreadNum; i++ ) {
				execDocBuilding.execute( luceneOutputter );
			}
		}
		finally {
			execDocBuilding.shutdown();
		}
	}

}
