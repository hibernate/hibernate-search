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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.CacheMode;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoScopeWorkspace;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;
import javax.persistence.metamodel.EntityType;
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
	private final long objectsLimit;
	private final int idFetchSize;
	private final Integer transactionTimeout;
	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();

	BatchCoordinator(HibernateOrmMassIndexingMappingContext mappingContext,
			DetachedBackendSessionContext sessionContext,
			MassIndexingNotifier notifier,
			Set<Class<?>> rootEntities, PojoScopeWorkspace scopeWorkspace,
			int typesToIndexInParallel, int documentBuilderThreads, CacheMode cacheMode,
			int objectLoadingBatchSize, long objectsLimit, boolean optimizeAtEnd,
			boolean purgeAtStart, boolean optimizeAfterPurge,
			int idFetchSize, Integer transactionTimeout) {
		super( notifier );
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
		this.objectsLimit = objectsLimit;
	}

	@Override
	public void runWithFailureHandler() throws InterruptedException {
		if ( !indexingFutures.isEmpty() ) {
			throw new AssertionFailure( "BatchCoordinator instance not expected to be reused" );
		}

		beforeBatch(); // purgeAll and pre-optimize activities
		doBatchWork();
		afterBatch();
	}

	@Override
	protected void cleanUpOnInterruption() throws InterruptedException {
		cancelPendingTasks();
		// Indexing performed before the exception must still be committed,
		// in order to leave the index in a consistent state
		afterBatchOnInterruption();
	}

	@Override
	protected void cleanUpOnFailure() {
		cancelPendingTasks();
	}

	@Override
	protected void notifySuccess() {
		getNotifier().notifyIndexingCompletedSuccessfully();
	}

	@Override
	protected void notifyInterrupted(InterruptedException exception) {
		getNotifier().notifyIndexingCompletedWithInterruption();
	}

	@Override
	protected void notifyFailure(RuntimeException exception) {
		getNotifier().notifyIndexingCompletedWithFailure( exception );
	}

	private void cancelPendingTasks() {
		for ( Future<?> task : indexingFutures ) {
			if ( !task.isDone() ) {
				task.cancel( true );
			}
		}
	}

	/**
	 * Will spawn a thread for each type in rootEntities, they will all re-join
	 * on endAllSignal when finished.
	 *
	 * @throws InterruptedException if interrupted while waiting for endAllSignal.
	 */
	private void doBatchWork() throws InterruptedException {
		ExecutorService executor = mappingContext.getThreadPoolProvider()
				.newFixedThreadPool( typesToIndexInParallel, MassIndexerImpl.THREAD_NAME_PREFIX + "Workspace" );
		for ( Class<?> type : rootEntities ) {
			indexingFutures.add( Futures.runAsync( createBatchIndexingWorkspace( type ), executor ) );
		}
		executor.shutdown();

		// Wait for the executor to finish
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf( indexingFutures.toArray( new CompletableFuture[0] ) )
		);
	}

	private <E> BatchIndexingWorkspace<E, ?> createBatchIndexingWorkspace(Class<E> indexedType) {
		EntityType<E> indexTypeModel = mappingContext.getSessionFactory().getMetamodel().entity( indexedType );
		String entityName = indexTypeModel.getName();
		SingularAttribute<? super E, ?> idAttributeOfIndexedType = indexTypeModel.getId( indexTypeModel.getIdType().getJavaType() );

		return new BatchIndexingWorkspace<>(
				mappingContext, sessionContext, getNotifier(),
				indexedType, entityName, idAttributeOfIndexedType,
				documentBuilderThreads, cacheMode,
				objectLoadingBatchSize,
				objectsLimit, idFetchSize, transactionTimeout
		);
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 */
	private void afterBatch() throws InterruptedException {
		if ( this.optimizeAtEnd ) {
			Futures.unwrappedExceptionGet( scopeWorkspace.optimize() );
		}
		Futures.unwrappedExceptionGet( scopeWorkspace.flush() );
	}

	/**
	 * batch indexing has been interrupted : flush to apply all index update realized before interruption
	 */
	private void afterBatchOnInterruption() throws InterruptedException {
		Futures.unwrappedExceptionGet( scopeWorkspace.flush() );
	}

	/**
	 * Optional operations to do before the multiple-threads start indexing
	 */
	private void beforeBatch() throws InterruptedException {
		if ( this.purgeAtStart ) {
			Futures.unwrappedExceptionGet( scopeWorkspace.purge() );
			if ( this.optimizeAfterPurge ) {
				Futures.unwrappedExceptionGet( scopeWorkspace.optimize() );
			}
		}
	}

}
