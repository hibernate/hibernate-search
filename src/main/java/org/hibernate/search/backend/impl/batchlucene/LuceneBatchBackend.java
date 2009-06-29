package org.hibernate.search.backend.impl.batchlucene;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.Environment;
import org.hibernate.search.SearchException;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.configuration.ConfigurationParseHelper;
import org.hibernate.search.backend.impl.lucene.DpSelectionVisitor;
import org.hibernate.search.backend.impl.lucene.PerDirectoryWorkProcessor;
import org.hibernate.search.batchindexing.IndexerProgressMonitor;
import org.hibernate.search.engine.DocumentBuilderIndexedEntity;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * First EXPERIMENTAL BatchBackend; this is not meant to be used as a regular
 * backend, only to apply batch changes to the index. Several threads
 * are used to make changes to each index, so order of Work processing is not guaranteed.
 * 
 * @author Sanne Grinovero
 */
public class LuceneBatchBackend implements BatchBackend {
	
	public static final String CONCURRENT_WRITERS = Environment.BATCH_BACKEND + ".concurrent_writers";

	private static final DpSelectionVisitor providerSelectionVisitor = new DpSelectionVisitor();

	private SearchFactoryImplementor searchFactoryImplementor;
	private final Map<DirectoryProvider<?>,DirectoryProviderWorkspace> resourcesMap = new HashMap<DirectoryProvider<?>,DirectoryProviderWorkspace>();
	private final PerDirectoryWorkProcessor asyncWorker = new AsyncBatchPerDirectoryWorkProcessor();
	private final PerDirectoryWorkProcessor syncWorker = new SyncBatchPerDirectoryWorkProcessor();

	public void initialize(Properties cfg, IndexerProgressMonitor monitor, SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
		int maxThreadsPerIndex = ConfigurationParseHelper.getIntValue( cfg, "concurrent_writers", 2 );
		if ( maxThreadsPerIndex < 1 ) {
			throw new SearchException( "concurrent_writers for batch backend must be at least 1." );
		}
		for ( DirectoryProvider<?> dp : searchFactoryImplementor.getDirectoryProviders() ) {
			DirectoryProviderWorkspace resources = new DirectoryProviderWorkspace( searchFactoryImplementor, dp, monitor, maxThreadsPerIndex );
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
			throw new SearchException( "Error while closing batch indexer", error );
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

}
