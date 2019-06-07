/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Executors;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @author Sanne Grinovero
 */
public class BatchIndexingWorkspace extends ErrorHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SessionFactoryImplementor sessionFactory;
	private final HibernateOrmMapping mapping;
	private final DetachedSessionContextImplementor sessionContext;

	private final ProducerConsumerQueue<List<Serializable>> primaryKeyStream;

	private final int documentBuilderThreads;
	private final Class<?> indexedType;
	private final String idNameOfIndexedType;

	// status control
	private final CountDownLatch producerEndSignal; //released when we stop adding Documents to Index
	private final CountDownLatch endAllSignal; //released when we release all locks and IndexWriter

	private final MassIndexingMonitor monitor;

	// loading options
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;

	private final long objectsLimit;

	private final int idFetchSize;
	private final Integer transactionTimeout;


	private final List<Future<?>> tasks = new ArrayList<>();

	public BatchIndexingWorkspace(SessionFactoryImplementor sessionFactory, HibernateOrmMapping mapping,
			DetachedSessionContextImplementor sessionContext,
			Class<?> type,
			int objectLoadingThreads, CacheMode cacheMode, int objectLoadingBatchSize,
			CountDownLatch endAllSignal, MassIndexingMonitor monitor, long objectsLimit,
			int idFetchSize, Integer transactionTimeout) {
		this.sessionFactory = sessionFactory;
		this.mapping = mapping;
		this.sessionContext = sessionContext;

		this.indexedType = type;
		this.idFetchSize = idFetchSize;
		this.transactionTimeout = transactionTimeout;

		this.idNameOfIndexedType = sessionFactory.getMetamodel().entity( indexedType ).locateIdAttribute().getName();


		//thread pool sizing:
		this.documentBuilderThreads = objectLoadingThreads;

		//loading options:
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;

		//pipelining queues:
		this.primaryKeyStream = new ProducerConsumerQueue<>( 1 );

		//end signal shared with other instances:
		this.endAllSignal = endAllSignal;
		this.producerEndSignal = new CountDownLatch( documentBuilderThreads );

		this.monitor = monitor;
		this.objectsLimit = objectsLimit;
	}

	@Override
	public void runWithErrorHandler() {
		if ( !tasks.isEmpty() ) {
			throw new AssertionFailure( "BatchIndexingWorkspace instance not expected to be reused - tasks should be empty" );
		}

		try {
			final BatchTransactionalContext transactionalContext = new BatchTransactionalContext( sessionFactory );
			//first start the consumers, then the producers (reverse order):
			//from primary keys to LuceneWork ADD operations:
			//TODO HSEARCH-3110 implement and pass the error handler
			startTransformationToLuceneWork();
			//from class definition to all primary keys:
			//TODO HSEARCH-3110 implement and pass the error handler
			startProducingPrimaryKeys( transactionalContext );
			try {
				producerEndSignal.await(); //await for all work being sent to the backend
				log.debugf( "All work for type %s has been produced", indexedType.getName() );
			}
			catch (InterruptedException e) {
				// on thread interruption cancel each pending task - thread executing the task must be interrupted
				for ( Future<?> task : tasks ) {
					if ( !task.isDone() ) {
						task.cancel( true );
					}
				}
				//restore interruption signal:
				Thread.currentThread().interrupt();
				throw log.interruptedBatchIndexingException( e );
			}
		}
		finally {
			endAllSignal.countDown();
		}
	}

	private void startProducingPrimaryKeys(BatchTransactionalContext transactionalContext) {
		final Runnable primaryKeyOutputter = new OptionallyWrapInJTATransaction(
				transactionalContext,
				new IdentifierProducer(
						primaryKeyStream, sessionFactory, objectLoadingBatchSize,
						indexedType, monitor, objectsLimit,
						idFetchSize, sessionContext.getTenantIdentifier()
				),
				transactionTimeout, sessionContext.getTenantIdentifier()
		);
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		final ThreadPoolExecutor execIdentifiersLoader = Executors.newFixedThreadPool( 1, "identifierloader" );
		try {
			tasks.add( execIdentifiersLoader.submit( primaryKeyOutputter ) );
		}
		finally {
			execIdentifiersLoader.shutdown();
		}
	}

	private void startTransformationToLuceneWork() {
		final Runnable documentOutputter = new IdentifierConsumerDocumentProducer(
				primaryKeyStream, monitor, sessionFactory,
				producerEndSignal, cacheMode, indexedType,
				idNameOfIndexedType, transactionTimeout,
				sessionContext.getTenantIdentifier(),
				mapping
		);
		final ThreadPoolExecutor execFirstLoader = Executors.newFixedThreadPool( documentBuilderThreads, "entityloader" );
		try {
			for ( int i = 0; i < documentBuilderThreads; i++ ) {
				tasks.add( execFirstLoader.submit( documentOutputter ) );
			}
		}
		finally {
			execFirstLoader.shutdown();
		}
	}
}
