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

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import org.hibernate.CacheMode;
import org.hibernate.SessionFactory;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.impl.batchlucene.BatchBackend;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.util.LoggerFactory;

/**
 * Makes sure that several different BatchIndexingWorkspace(s)
 * can be started concurrently, sharing the same batch-backend
 * and IndexWriters.
 *
 * @author Sanne Grinovero
 */
public class BatchCoordinator implements Runnable {

	private static final Logger log = LoggerFactory.make();

	private final Class<?>[] rootEntities; //entity types to reindex excluding all subtypes of each-other
	private final SearchFactoryImplementor searchFactoryImplementor;
	private final SessionFactory sessionFactory;
	private final int objectLoadingThreads;
	private final int collectionLoadingThreads;
	private final CacheMode cacheMode;
	private final int objectLoadingBatchSize;
	private final Integer writerThreads; //could be null: in case global configuration properties are applied
	private final boolean optimizeAtEnd;
	private final boolean purgeAtStart;
	private final boolean optimizeAfterPurge;
	private final CountDownLatch endAllSignal;
	private final MassIndexerProgressMonitor monitor;
	private final long objectsLimit;

	private BatchBackend backend;

	public BatchCoordinator(Set<Class<?>> rootEntities,
							SearchFactoryImplementor searchFactoryImplementor,
							SessionFactory sessionFactory, int objectLoadingThreads,
							int collectionLoadingThreads, CacheMode cacheMode,
							int objectLoadingBatchSize, long objectsLimit,
							boolean optimizeAtEnd,
							boolean purgeAtStart, boolean optimizeAfterPurge,
							MassIndexerProgressMonitor monitor, Integer writerThreads) {
		this.rootEntities = rootEntities.toArray( new Class<?>[rootEntities.size()] );
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
		this.writerThreads = writerThreads;
		this.endAllSignal = new CountDownLatch( rootEntities.size() );
	}

	public void run() {
		backend = searchFactoryImplementor.makeBatchBackend( monitor, writerThreads );
		try {
			beforeBatch(); // purgeAll and pre-optimize activities
			doBatchWork();
			backend.stopAndFlush( 60L * 60 * 24, TimeUnit.SECONDS ); //1 day : enough to flush to indexes?
//			backend.stopAndFlush( 10, TimeUnit.SECONDS );
			afterBatch();
		}
		catch ( InterruptedException e ) {
			log.error( "Batch indexing was interrupted" );
			Thread.currentThread().interrupt();
		}
		finally {
			backend.close();
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
		ExecutorService executor = Executors.newFixedThreadPool( rootEntities.length, "BatchIndexingWorkspace" );
		for ( Class<?> type : rootEntities ) {
			executor.execute(
					new BatchIndexingWorkspace(
							searchFactoryImplementor, sessionFactory, type,
							objectLoadingThreads, collectionLoadingThreads,
							cacheMode, objectLoadingBatchSize,
							endAllSignal, monitor, backend, objectsLimit
					)
			);
		}
		executor.shutdown();
		endAllSignal.await(); //waits for the executor to finish
	}

	/**
	 * Operations to do after all subthreads finished their work on index
	 */
	private void afterBatch() {
		if ( this.optimizeAtEnd ) {
			Set<Class<?>> targetedClasses = searchFactoryImplementor.getIndexedTypesPolymorphic( rootEntities );
			optimize( targetedClasses );
		}
	}

	/**
	 * Optional operations to do before the multiple-threads start indexing
	 */
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
