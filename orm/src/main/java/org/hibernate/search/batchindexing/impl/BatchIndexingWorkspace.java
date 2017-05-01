/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.util.impl.Executors;
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

	private final ProducerConsumerQueue<List<Serializable>> primaryKeyStream;

	private final int documentBuilderThreads;
	private final IndexedTypeIdentifier indexedType;
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
	private final Integer transactionTimeout;

	private final String tenantId;

	private final List<Future<?>> tasks = new ArrayList<>();

	public BatchIndexingWorkspace(ExtendedSearchIntegrator extendedIntegrator,
								SessionFactoryImplementor sessionFactory,
								IndexedTypeIdentifier type,
								int objectLoadingThreads,
								CacheMode cacheMode,
								int objectLoadingBatchSize,
								CountDownLatch endAllSignal,
								MassIndexerProgressMonitor monitor,
								BatchBackend backend,
								long objectsLimit,
								int idFetchSize,
								Integer transactionTimeout,
								String tenantId) {
		super( extendedIntegrator );
		this.indexedType = type;
		this.idFetchSize = idFetchSize;
		this.transactionTimeout = transactionTimeout;
		this.tenantId = tenantId;
		this.idNameOfIndexedType = extendedIntegrator.getIndexBinding( type )
				.getDocumentBuilder()
				.getIdPropertyName();
		this.sessionFactory = sessionFactory;

		//thread pool sizing:
		this.documentBuilderThreads = objectLoadingThreads;

		//loading options:
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;
		this.backend = backend;

		//pipelining queues:
		this.primaryKeyStream = new ProducerConsumerQueue<List<Serializable>>( 1 );

		//end signal shared with other instances:
		this.endAllSignal = endAllSignal;
		this.producerEndSignal = new CountDownLatch( documentBuilderThreads );

		this.monitor = monitor;
		this.objectsLimit = objectsLimit;
	}

	@Override
	public void runWithErrorHandler() {
		if ( tasks.size() > 0 ) {
			throw new AssertionFailure( "BatchIndexingWorkspace instance not expected to be reused - tasks should be empty" );
		}

		try {
			final ErrorHandler errorHandler = extendedIntegrator.getErrorHandler();
			final BatchTransactionalContext transactionalContext = new BatchTransactionalContext( extendedIntegrator, sessionFactory, errorHandler, tenantId );
			//first start the consumers, then the producers (reverse order):
			//from primary keys to LuceneWork ADD operations:
			startTransformationToLuceneWork( transactionalContext, errorHandler );
			//from class definition to all primary keys:
			startProducingPrimaryKeys( transactionalContext, errorHandler );
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
				throw new SearchException( "Interrupted on batch Indexing; index will be left in unknown state!" );
			}
		}
		finally {
			endAllSignal.countDown();
		}
	}

	private void startProducingPrimaryKeys(BatchTransactionalContext transactionalContext, ErrorHandler errorHandler) {
		final Runnable primaryKeyOutputter = new OptionallyWrapInJTATransaction( transactionalContext,
				new IdentifierProducer(
						primaryKeyStream, sessionFactory,
						objectLoadingBatchSize, indexedType, monitor,
						objectsLimit, errorHandler, idFetchSize,
						tenantId
				),
				transactionTimeout,
				tenantId );
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		final ThreadPoolExecutor execIdentifiersLoader = Executors.newFixedThreadPool( 1, "identifierloader" );
		try {
			tasks.add( execIdentifiersLoader.submit( primaryKeyOutputter ) );
		}
		finally {
			execIdentifiersLoader.shutdown();
		}
	}

	private void startTransformationToLuceneWork(BatchTransactionalContext transactionalContext, ErrorHandler errorHandler) {
		final Runnable documentOutputter = new IdentifierConsumerDocumentProducer(
				primaryKeyStream, monitor, sessionFactory, producerEndSignal,
				cacheMode, indexedType, extendedIntegrator,
				idNameOfIndexedType, backend, errorHandler,
				transactionTimeout, tenantId
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
