/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.massindexing.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;

import javax.persistence.metamodel.SingularAttribute;

import org.hibernate.CacheMode;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.massindexing.monitor.MassIndexingMonitor;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * This runnable will prepare a pipeline for batch indexing
 * of entities, managing the lifecycle of several ThreadPools.
 *
 * @param <E> The entity type
 * @param <I> The identifier type
 *
 * @author Sanne Grinovero
 */
public class BatchIndexingWorkspace<E, I> extends FailureHandledRunnable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final HibernateOrmMassIndexingMappingContext mappingContext;
	private final DetachedBackendSessionContext sessionContext;

	private final ProducerConsumerQueue<List<I>> primaryKeyStream;

	private final int documentBuilderThreads;
	private final Class<E> indexedType;
	private final String entityName;
	private final SingularAttribute<? super E, I> idAttributeOfIndexedType;

	private final MassIndexingMonitor monitor;

	// loading options
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;

	private final long objectsLimit;

	private final int idFetchSize;
	private final Integer transactionTimeout;

	private final List<CompletableFuture<?>> identifierProducingFutures = new ArrayList<>();
	private final List<CompletableFuture<?>> indexingFutures = new ArrayList<>();

	BatchIndexingWorkspace(HibernateOrmMassIndexingMappingContext mappingContext,
			DetachedBackendSessionContext sessionContext,
			Class<E> type, String entityName, SingularAttribute<? super E, I> idAttributeOfIndexedType,
			int objectLoadingThreads, CacheMode cacheMode, int objectLoadingBatchSize,
			MassIndexingMonitor monitor, FailureHandler failureHandler,
			long objectsLimit,
			int idFetchSize, Integer transactionTimeout) {
		super( failureHandler );
		this.mappingContext = mappingContext;
		this.sessionContext = sessionContext;

		this.indexedType = type;
		this.entityName = entityName;
		this.idAttributeOfIndexedType = idAttributeOfIndexedType;
		this.idFetchSize = idFetchSize;
		this.transactionTimeout = transactionTimeout;

		//thread pool sizing:
		this.documentBuilderThreads = objectLoadingThreads;

		//loading options:
		this.cacheMode = cacheMode;
		this.objectLoadingBatchSize = objectLoadingBatchSize;

		//pipelining queues:
		this.primaryKeyStream = new ProducerConsumerQueue<>( 1 );

		this.monitor = monitor;

		this.objectsLimit = objectsLimit;
	}

	@Override
	public void runWithFailureHandler() throws InterruptedException {
		if ( !identifierProducingFutures.isEmpty() || !indexingFutures.isEmpty() ) {
			throw new AssertionFailure( "BatchIndexingWorkspace instance not expected to be reused" );
		}

		final BatchTransactionalContext transactionalContext =
				new BatchTransactionalContext( mappingContext.getSessionFactory() );
		// First start the consumers, then the producers (reverse order):
		startIndexing();
		startProducingPrimaryKeys( transactionalContext );
		// Wait for indexing to finish.
		Futures.unwrappedExceptionGet(
				CompletableFuture.allOf( indexingFutures.toArray( new CompletableFuture[0] ) )
						// Exceptions are handled by each runnable
						.exceptionally( ignored -> null )
		);
		log.debugf( "Indexing for %s is done", indexedType.getName() );
	}

	@Override
	protected void cleanUpOnInterruption() {
		cancelPendingTasks();
	}

	@Override
	protected void cleanUpOnFailure() {
		cancelPendingTasks();
	}

	private void cancelPendingTasks() {
		// Cancel each pending task - threads executing the tasks must be interrupted
		for ( Future<?> task : identifierProducingFutures ) {
			task.cancel( true );
		}
		for ( Future<?> task : indexingFutures ) {
			task.cancel( true );
		}
	}

	private void startProducingPrimaryKeys(BatchTransactionalContext transactionalContext) {
		final Runnable primaryKeyOutputter = new OptionallyWrapInJTATransaction(
				transactionalContext,
				getFailureHandler(),
				new IdentifierProducer<>(
						primaryKeyStream, mappingContext.getSessionFactory(), objectLoadingBatchSize,
						indexedType, entityName, idAttributeOfIndexedType,
						monitor, getFailureHandler(),
						objectsLimit,
						idFetchSize, sessionContext.getTenantIdentifier()
				),
				transactionTimeout, sessionContext.getTenantIdentifier()
		);
		//execIdentifiersLoader has size 1 and is not configurable: ensures the list is consistent as produced by one transaction
		final ThreadPoolExecutor identifierProducingExecutor = mappingContext.getThreadPoolProvider()
				.newFixedThreadPool( 1, MassIndexerImpl.THREAD_NAME_PREFIX + entityName + " - ID loading" );
		try {
			identifierProducingFutures.add( Futures.runAsync( primaryKeyOutputter, identifierProducingExecutor ) );
		}
		finally {
			identifierProducingExecutor.shutdown();
		}
	}

	private void startIndexing() {
		final Runnable documentOutputter = new IdentifierConsumerDocumentProducer<>(
				primaryKeyStream,
				monitor, getFailureHandler(),
				mappingContext,
				cacheMode,
				indexedType, entityName, idAttributeOfIndexedType,
				transactionTimeout,
				sessionContext.getTenantIdentifier()
		);
		final ThreadPoolExecutor indexingExecutor = mappingContext.getThreadPoolProvider()
				.newFixedThreadPool( documentBuilderThreads, MassIndexerImpl.THREAD_NAME_PREFIX + entityName + " - Entity loading" );
		try {
			for ( int i = 0; i < documentBuilderThreads; i++ ) {
				indexingFutures.add( Futures.runAsync( documentOutputter, indexingExecutor ) );
			}
		}
		finally {
			indexingExecutor.shutdown();
		}
	}
}
