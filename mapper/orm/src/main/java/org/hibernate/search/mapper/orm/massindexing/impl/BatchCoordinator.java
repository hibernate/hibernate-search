/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Executors;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import javax.persistence.metamodel.IdentifiableType;
import javax.persistence.metamodel.SingularAttribute;

/**
 * Makes sure that several different BatchIndexingWorkspace(s)
 * can be started concurrently, sharing the same batch-backend
 * and IndexWriters.
 *
 * @author Sanne Grinovero
 */
public class BatchCoordinator extends FailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final DetachedBackendSessionContext sessionContext;
	private final Set<Class<?>> rootEntities; //entity types to reindex excluding all subtypes of each-other
	private final PojoScopeWorkspace scopeWorkspace;

	private final int typesToIndexInParallel;
	private final int documentBuilderThreads;
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;
	private final boolean optimizeAtEnd;
	private final boolean purgeAtStart;
	private final boolean optimizeAfterPurge;
	private final CountDownLatch endAllSignal;
	private final MassIndexingMonitor monitor;
	private final long objectsLimit;
	private final int idFetchSize;
	private final Integer transactionTimeout;
	private final List<Future<?>> indexingTasks = new ArrayList<>();

	public BatchCoordinator(HibernateOrmMassIndexingMappingContext mappingContext,
			DetachedBackendSessionContext sessionContext,
			Set<Class<?>> rootEntities, PojoScopeWorkspace scopeWorkspace,
			int typesToIndexInParallel, int documentBuilderThreads, CacheMode cacheMode,
			int objectLoadingBatchSize, long objectsLimit, boolean optimizeAtEnd,
			boolean purgeAtStart, boolean optimizeAfterPurge,
			MassIndexingMonitor monitor,
			int idFetchSize, Integer transactionTimeout) {
		super( mappingContext.getFailureHandler() );
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;
		this.rootEntities = rootEntities;
		this.scopeWorkspace = scopeWorkspace;

		this.idFetchSize = idFetchSize;
		this.transactionTimeout = transactionTimeout;
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
	public void runWithFailureHandler() {
		if ( !indexingTasks.isEmpty() ) {
			throw new AssertionFailure( "BatchCoordinator instance not expected to be reused - indexingTasks should be empty" );
		}

		try {
			beforeBatch(); // purgeAll and pre-optimize activities
			doBatchWork();
			afterBatch();
		}
		catch (InterruptedException e) {
			log.interruptedBatchIndexing();
			// on thread interruption cancel each pending task - thread executing the task must be interrupted
			for ( Future<?> task : indexingTasks ) {
				if ( !task.isDone() ) {
					task.cancel( true );
				}
			}
			// try afterBatch stuff - indexation realized before interruption will be commited - index should be in a
			// coherent state (not corrupted)
			afterBatchOnInterruption();

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
	 *
	 * @throws InterruptedException if interrupted while waiting for endAllSignal.
	 */
	private void doBatchWork() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool( typesToIndexInParallel, "BatchIndexingWorkspace" );
		for ( Class<?> type : rootEntities ) {
			indexingTasks.add( executor.submit( createBatchIndexingWorkspace( type ) ) );
		}
		executor.shutdown();
		endAllSignal.await(); //waits for the executor to finish
	}

	private <E> BatchIndexingWorkspace<E, ?> createBatchIndexingWorkspace(Class<E> indexedType) {
		IdentifiableType<E> indexTypeModel = mappingContext.getSessionFactory().getMetamodel().entity( indexedType );
		SingularAttribute<? super E, ?> idAttributeOfIndexedType = indexTypeModel.getId( indexTypeModel.getIdType().getJavaType() );

		return new BatchIndexingWorkspace<>(
				mappingContext, sessionContext,
				indexedType, idAttributeOfIndexedType,
				documentBuilderThreads, cacheMode,
				objectLoadingBatchSize, endAllSignal,
				monitor, getFailureHandler(),
				objectsLimit, idFetchSize, transactionTimeout
		);
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 */
	private void afterBatch() {
		if ( this.optimizeAtEnd ) {
			Futures.unwrappedExceptionJoin( scopeWorkspace.optimize() );
		}
		Futures.unwrappedExceptionJoin( scopeWorkspace.flush() );
	}

	/**
	 * batch indexing has been interrupted : flush to apply all index update realized before interruption
	 */
	private void afterBatchOnInterruption() {
		Futures.unwrappedExceptionJoin( scopeWorkspace.flush() );
	}

	/**
	 * Optional operations to do before the multiple-threads start indexing
	 */
	private void beforeBatch() {
		if ( this.purgeAtStart ) {
			Futures.unwrappedExceptionJoin( scopeWorkspace.purge() );
			if ( this.optimizeAfterPurge ) {
				Futures.unwrappedExceptionJoin( scopeWorkspace.optimize() );
			}
		}
	}

}
