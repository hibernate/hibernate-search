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
package org.hibernate.search.backend.impl.batchlucene;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.Environment;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.backend.impl.lucene.DpSelectionVisitor;
import org.hibernate.search.backend.impl.lucene.PerDirectoryWorkProcessor;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This is not meant to be used as a regular
 * backend, only to apply batch changes to the index. Several threads
 * are used to make changes to each index, so order of Work processing is not guaranteed.
 * 
 * @author Sanne Grinovero
 * @experimental First {@code BatchBackend}
 */
public class LuceneBatchBackend implements BatchBackend {
	
	public static final String CONCURRENT_WRITERS = Environment.BATCH_BACKEND + ".concurrent_writers";

	private static final DpSelectionVisitor providerSelectionVisitor = new DpSelectionVisitor();

	private SearchFactoryImplementor searchFactoryImplementor;
	private final Map<DirectoryProvider<?>,DirectoryProviderWorkspace> resourcesMap = new HashMap<DirectoryProvider<?>,DirectoryProviderWorkspace>();
	private final PerDirectoryWorkProcessor asyncWorker = new AsyncBatchPerDirectoryWorkProcessor();
	private final PerDirectoryWorkProcessor syncWorker = new SyncBatchPerDirectoryWorkProcessor();

	public void initialize(Properties cfg, MassIndexerProgressMonitor monitor, WorkerBuildContext context) {
		this.searchFactoryImplementor = context.getUninitializedSearchFactory();
		final int maxThreadsPerIndex = definedIndexWriters( cfg );
		ErrorHandler errorHandler = searchFactoryImplementor.getErrorHandler();
		for ( DirectoryProvider<?> dp : context.getDirectoryProviders() ) {
			DirectoryProviderWorkspace resources = new DirectoryProviderWorkspace( context, dp, monitor, maxThreadsPerIndex, errorHandler );
			resourcesMap.put( dp, resources );
		}
	}

	public void enqueueAsyncWork(LuceneWork work) throws InterruptedException {
		sendWorkToShards( work, asyncWorker );
	}

	public void doWorkInSync(LuceneWork work) {
		try {
			sendWorkToShards( work, syncWorker );
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			//doesn't happen, see SyncBatchPerDirectoryWorkProcessor below: is missing the throws.
			throw new SearchException( "AssertionFailure" );
		}
	}

	/**
	 * Stops the background threads and flushes changes;
	 * Please note the timeout is applied to each index in
	 * sequence, so it might take as much time as timeout*directoryproviders
	 */
	public void stopAndFlush(long timeout, TimeUnit unit) throws InterruptedException {
		for ( DirectoryProviderWorkspace res : resourcesMap.values() ) {
			res.stopAndFlush( timeout, unit );
		}
	}
	
	public void close() {
		Throwable error = null;
		for ( DirectoryProviderWorkspace res : resourcesMap.values() ) {
			try {
				res.close();
			}
			catch (Throwable t) {
				//make sure to try closing all IndexWriters
				error = t;
			}
		}
		if ( error != null ) {
			throw new SearchException( "Error while closing massindexer", error );
		}
	}
	
	private void sendWorkToShards(LuceneWork work, PerDirectoryWorkProcessor worker) throws InterruptedException {
		final Class<?> entityType = work.getEntityClass();
		DocumentBuilderIndexedEntity<?> documentBuilder = searchFactoryImplementor.getDocumentBuilderIndexedEntity( entityType );
		IndexShardingStrategy shardingStrategy = documentBuilder.getDirectoryProviderSelectionStrategy();
		work.getWorkDelegate( providerSelectionVisitor ).addAsPayLoadsToQueue( work, shardingStrategy, worker );
	}

	/**
	 * Implements a PerDirectoryWorkProcessor to enqueue work Asynchronously.
	 */
	private class AsyncBatchPerDirectoryWorkProcessor implements PerDirectoryWorkProcessor {

		public void addWorkToDpProcessor(DirectoryProvider<?> dp, LuceneWork work) throws InterruptedException {
			resourcesMap.get( dp ).enqueueAsyncWork( work );
		}
		
	}
	
	/**
	 * Implements a PerDirectoryWorkProcessor to enqueue work Synchronously.
	 */
	private class SyncBatchPerDirectoryWorkProcessor implements PerDirectoryWorkProcessor {

		public void addWorkToDpProcessor(DirectoryProvider<?> dp, LuceneWork work) {
			resourcesMap.get( dp ).doWorkInSync( work );
		}
		
	}
	
	/**
	 * @param cfg Configuration properties
	 * @return the number of threads to be writing on the shared IndexWriter
	 */
	private static int definedIndexWriters(Properties cfg) {
		final int indexWriters = ConfigurationParseHelper.getIntValue( cfg, "concurrent_writers", 2 );
		if ( indexWriters < 1 ) {
			throw new SearchException( "concurrent_writers for batch backend must be at least 1." );
		}
		return indexWriters;
	}

}
