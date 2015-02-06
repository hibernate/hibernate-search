/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batchindexing.impl;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.CacheMode;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
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

	private final Class<?>[] rootEntities; //entity types to reindex excluding all subtypes of each-other
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
	private final String tenantId;

	public BatchCoordinator(Set<Class<?>> rootEntities,
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
							String tenantId) {
		super( extendedIntegrator );
		this.idFetchSize = idFetchSize;
		this.tenantId = tenantId;
		this.rootEntities = rootEntities.toArray( new Class<?>[rootEntities.size()] );
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
		try {
			beforeBatch( backend ); // purgeAll and pre-optimize activities
			doBatchWork( backend );
			afterBatch( backend );
		}
		catch (InterruptedException e) {
			log.interruptedBatchIndexing();
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
	 */
	private void doBatchWork(BatchBackend backend) throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool( typesToIndexInParallel, "BatchIndexingWorkspace" );
		for ( Class<?> type : rootEntities ) {
			executor.execute(
					new BatchIndexingWorkspace(
							extendedIntegrator, sessionFactory, type,
							documentBuilderThreads,
							cacheMode, objectLoadingBatchSize, endAllSignal,
							monitor, backend, objectsLimit, idFetchSize, tenantId
					)
			);
		}
		executor.shutdown();
		endAllSignal.await(); //waits for the executor to finish
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 * @param backend
	 */
	private void afterBatch(BatchBackend backend) {
		Set<Class<?>> targetedClasses = extendedIntegrator.getIndexedTypesPolymorphic( rootEntities );
		if ( this.optimizeAtEnd ) {
			backend.optimize( targetedClasses );
		}
		backend.flush( targetedClasses );
	}

	/**
	 * Optional operations to do before the multiple-threads start indexing
	 * @param backend
	 */
	private void beforeBatch(BatchBackend backend) {
		if ( this.purgeAtStart ) {
			//purgeAll for affected entities
			Set<Class<?>> targetedClasses = extendedIntegrator.getIndexedTypesPolymorphic( rootEntities );
			for ( Class<?> clazz : targetedClasses ) {
				//needs do be in-sync work to make sure we wait for the end of it.
				backend.doWorkInSync( new PurgeAllLuceneWork( clazz ) );
			}
			if ( this.optimizeAfterPurge ) {
				backend.optimize( targetedClasses );
			}
		}
	}

}
