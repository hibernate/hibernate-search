/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.AssertionFailure;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Makes sure that several different BatchIndexingWorkspace(s)
 * can be started concurrently, sharing the same batch-backend
 * and IndexWriters.
 *
 * @author Sanne Grinovero
 */
public class BatchCoordinator extends ErrorHandledRunnable {

	private static final Log log = LoggerFactory.make();

	private final IndexedTypeSet rootEntities; //entity types to reindex excluding all subtypes of each-other
	private final SessionFactoryImplementor sessionFactory;
	private final int typesToIndexInParallel;
	private final int documentBuilderThreads;
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;
	private final boolean optimizeAtEnd;
	private final boolean purgeAtStart;
	private final boolean optimizeAfterPurge;
	private final CountDownLatch endAllSignal;
	private final MassIndexerProgressMonitor monitor;
	private final long objectsLimit;
	private final int idFetchSize;
	private final Integer transactionTimeout;
	private final String tenantId;
	private final List<Future<?>> indexingTasks = new ArrayList<>();

	public BatchCoordinator(IndexedTypeSet rootEntities,
							ExtendedSearchIntegrator extendedIntegrator,
							SessionFactoryImplementor sessionFactory,
							int typesToIndexInParallel,
							int documentBuilderThreads,
							CacheMode cacheMode,
							int objectLoadingBatchSize,
							long objectsLimit,
							boolean optimizeAtEnd,
							boolean purgeAtStart,
							boolean optimizeAfterPurge,
							MassIndexerProgressMonitor monitor,
							int idFetchSize,
							Integer transactionTimeout,
							String tenantId) {
		super( extendedIntegrator );
		this.idFetchSize = idFetchSize;
		this.transactionTimeout = transactionTimeout;
		this.tenantId = tenantId;
		this.rootEntities = rootEntities;
		this.sessionFactory = sessionFactory;
		this.typesToIndexInParallel = typesToIndexInParallel;
		this.documentBuilderThreads = documentBuilderThreads;
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;
		this.optimizeAtEnd = optimizeAtEnd;
		this.purgeAtStart = purgeAtStart;
		this.optimizeAfterPurge = optimizeAfterPurge;
		this.monitor = monitor;
		this.objectsLimit = objectsLimit;
		this.endAllSignal = new CountDownLatch( rootEntities.size() );
	}

	@Override
	public void runWithErrorHandler() {
		final BatchBackend backend = extendedIntegrator.makeBatchBackend( monitor );
		if ( indexingTasks.size() > 0 ) {
			throw new AssertionFailure( "BatchCoordinator instance not expected to be reused - indexingTasks should be empty" );
		}
		try {
			beforeBatch( backend ); // purgeAll and pre-optimize activities
			doBatchWork( backend );
			afterBatch( backend );
		}
		catch (InterruptedException | ExecutionException e) {
			log.interruptedBatchIndexing();
			// on thread interruption cancel each pending task - thread executing the task must be interrupted
			for ( Future<?> task : indexingTasks ) {
				if ( !task.isDone() ) {
					task.cancel( true );
				}
			}
			// try afterBatch stuff - indexation realized before interruption will be commited - index should be in a
			// coherent state (not corrupted)
			afterBatchOnInterruption( backend );

			// restore interruption signal:
			Thread.currentThread().interrupt();
		}
		finally {
			monitor.indexingCompleted();
		}
	}

	/**
	 * Will spawn a thread for each type in rootEntities, they will all re-join
	 * on endAllSignal when finished.
	 * @param backend
	 *
	 * @throws InterruptedException if interrupted while waiting for endAllSignal.
	 * @throws ExecutionException
	 */
	private void doBatchWork(BatchBackend backend) throws InterruptedException, ExecutionException {
		ExecutorService executor = Executors.newFixedThreadPool( typesToIndexInParallel, "BatchIndexingWorkspace" );
		for ( IndexedTypeIdentifier type : rootEntities ) {
			indexingTasks.add( executor.submit( new BatchIndexingWorkspace( extendedIntegrator, sessionFactory, type, documentBuilderThreads, cacheMode,
					objectLoadingBatchSize, endAllSignal, monitor, backend, objectsLimit, idFetchSize, transactionTimeout, tenantId ) ) );

		}
		executor.shutdown();
		endAllSignal.await(); //waits for the executor to finish
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 * @param backend
	 */
	private void afterBatch(BatchBackend backend) {
		IndexedTypeSet targetedClasses = extendedIntegrator.getIndexedTypesPolymorphic( rootEntities );
		if ( this.optimizeAtEnd ) {
			backend.optimize( targetedClasses );
		}
		backend.flush( targetedClasses );
	}

	/**
	 * batch indexing has been interrupted : flush to apply all index update realized before interruption
	 *
	 * @param backend
	 */
	private void afterBatchOnInterruption(BatchBackend backend) {
		IndexedTypeSet targetedClasses = extendedIntegrator.getIndexedTypesPolymorphic( rootEntities );
		backend.flush( targetedClasses );
	}

	/**
	 * Optional operations to do before the multiple-threads start indexing
	 * @param backend
	 */
	private void beforeBatch(BatchBackend backend) {
		if ( this.purgeAtStart ) {
			//purgeAll for affected entities
			IndexedTypeSet targetedClasses = extendedIntegrator.getIndexedTypesPolymorphic( rootEntities );
			for ( IndexedTypeIdentifier type : targetedClasses ) {
				//needs do be in-sync work to make sure we wait for the end of it.
				backend.doWorkInSync( new PurgeAllLuceneWork( tenantId, type ) );
			}
			if ( this.optimizeAfterPurge ) {
				backend.optimize( targetedClasses );
			}
		}
	}

}
