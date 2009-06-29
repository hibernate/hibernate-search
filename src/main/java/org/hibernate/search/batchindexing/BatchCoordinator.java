package org.hibernate.search.batchindexing;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Makes sure that several different BatchIndexingWorkspace(s)
 * can be started concurrently, sharing the same batch-backend
 * and IndexWriters.
 * 
 * @author Sanne Grinovero
 */
public class BatchCoordinator implements Runnable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final Class<?>[] rootEntities;
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final SessionFactory sessionFactory;
	private final int objectLoadingThreads;
	private final int collectionLoadingThreads;
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;
	private final boolean optimizeAtEnd;
	private final boolean purgeAtStart;
	private final boolean optimizeAfterPurge;
	private final CountDownLatch endAllSignal;
	private final IndexerProgressMonitor monitor;
	private final int objectsLimit;
	
	private BatchBackend backend;

	public BatchCoordinator(Set<Class<?>> rootEntities,
			SearchFactoryImplementor searchFactoryImplementor,
			SessionFactory sessionFactory, int objectLoadingThreads,
			int collectionLoadingThreads, CacheMode cacheMode,
			int objectLoadingBatchSize, int objectsLimit,
			boolean optimizeAtEnd,
			boolean purgeAtStart, boolean optimizeAfterPurge,
			IndexerProgressMonitor monitor) {
				this.rootEntities = rootEntities.toArray( new Class<?>[ rootEntities.size() ] );
				this.searchFactoryImplementor = searchFactoryImplementor;
				this.sessionFactory = sessionFactory;
				this.objectLoadingThreads = objectLoadingThreads;
				this.collectionLoadingThreads = collectionLoadingThreads;
				this.cacheMode = cacheMode;
				this.objectLoadingBatchSize = objectLoadingBatchSize;
				this.optimizeAtEnd = optimizeAtEnd;
				this.purgeAtStart = purgeAtStart;
				this.optimizeAfterPurge = optimizeAfterPurge;
				this.monitor = monitor;
				this.objectsLimit = objectsLimit;
				this.endAllSignal = new CountDownLatch( rootEntities.size() );
	}

	public void run() {
		backend = searchFactoryImplementor.makeBatchBackend( monitor );
		try {
			beforeBatch(); // purgeAll and pre-optimize activities
			doBatchWork();
			backend.stopAndFlush( 60L*60*24, TimeUnit.SECONDS ); //1 day : enough to flush to indexes?
//			backend.stopAndFlush( 10, TimeUnit.SECONDS );
			afterBatch();
		} catch (InterruptedException e) {
			log.error( "Batch indexing was interrupted" );
			Thread.currentThread().interrupt();
		}
		finally {
			backend.close();
		}
	}

	private void doBatchWork() throws InterruptedException {
		ExecutorService executor = Executors.newFixedThreadPool( rootEntities.length, "BatchIndexingWorkspace" );
		for ( Class<?> type : rootEntities ) {
			executor.execute( new BatchIndexingWorkspace(
					searchFactoryImplementor, sessionFactory, type,
					objectLoadingThreads, collectionLoadingThreads,
					cacheMode, objectLoadingBatchSize,
					endAllSignal, monitor, backend, objectsLimit ) );
		}
		executor.shutdown();
		endAllSignal.await(); //waits for the executor to finish
	}

	private void afterBatch() {
		if ( this.optimizeAtEnd ) {
			Set<Class<?>> targetedClasses = searchFactoryImplementor.getIndexedTypesPolymorphic( rootEntities );
			optimize( targetedClasses );
		}
	}

	private void beforeBatch() {
		if ( this.purgeAtStart ) {
			//purgeAll for affected entities
			Set<Class<?>> targetedClasses = searchFactoryImplementor.getIndexedTypesPolymorphic( rootEntities );
			for ( Class<?> clazz : targetedClasses ) {
				//needs do be in-sync work to make sure we wait for the end of it.
				backend.doWorkInSync( new PurgeAllLuceneWork( clazz ) ); 
			}
			if ( this.optimizeAfterPurge ) {
				optimize( targetedClasses );
			}
		}
	}

	private void optimize(Set<Class<?>> targetedClasses) {
		for ( Class<?> clazz : targetedClasses ) {
			//TODO the backend should remove duplicate optimize work to the same DP (as entities might share indexes)
			backend.doWorkInSync( new OptimizeLuceneWork( clazz ) );
		}
	}
	
}
