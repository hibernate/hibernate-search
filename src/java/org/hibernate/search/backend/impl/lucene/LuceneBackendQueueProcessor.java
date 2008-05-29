//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ArrayList;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.Workspace;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.engine.DocumentBuilder;
import org.hibernate.search.store.DirectoryProvider;
import org.hibernate.search.store.IndexShardingStrategy;
import org.hibernate.annotations.common.AssertionFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apply the operations to Lucene directories avoiding deadlocks.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 */
public class LuceneBackendQueueProcessor implements Runnable {
	
	/**
	 * Class logger.
	 */
	private static final Logger log = LoggerFactory.getLogger( LuceneBackendQueueProcessor.class );
	
	private final List<LuceneWork> queue;
	private final SearchFactoryImplementor searchFactoryImplementor;

	public LuceneBackendQueueProcessor(List<LuceneWork> queue, SearchFactoryImplementor searchFactoryImplementor) {
		this.queue = queue;
		this.searchFactoryImplementor = searchFactoryImplementor;
	}

	public void run() {
		Workspace workspace;
		LuceneWorker worker;
		workspace = new Workspace( searchFactoryImplementor );
		worker = new LuceneWorker( workspace );
		try {
			List<LuceneWorker.WorkWithPayload> queueWithFlatDPs = new ArrayList<LuceneWorker.WorkWithPayload>( queue.size()*2 );
			for ( LuceneWork work : queue ) {
				DocumentBuilder documentBuilder = searchFactoryImplementor.getDocumentBuilders().get( work.getEntityClass() );
				IndexShardingStrategy shardingStrategy = documentBuilder.getDirectoryProviderSelectionStrategy();

				if ( PurgeAllLuceneWork.class.isAssignableFrom( work.getClass() ) ) {
					DirectoryProvider[] providers = shardingStrategy.getDirectoryProvidersForDeletion(
							work.getEntityClass(),
							work.getId(),
							work.getIdInString()
					);
					for (DirectoryProvider provider : providers) {
						queueWithFlatDPs.add( new LuceneWorker.WorkWithPayload( work, provider ) );
					}
				}
				else if ( AddLuceneWork.class.isAssignableFrom( work.getClass() ) ) {
					DirectoryProvider provider = shardingStrategy.getDirectoryProviderForAddition(
							work.getEntityClass(),
							work.getId(),
							work.getIdInString(),
							work.getDocument()
					);
					queueWithFlatDPs.add( new LuceneWorker.WorkWithPayload( work, provider ) );
				}
				else if ( DeleteLuceneWork.class.isAssignableFrom( work.getClass() ) ) {
					DirectoryProvider[] providers = shardingStrategy.getDirectoryProvidersForDeletion(
							work.getEntityClass(),
							work.getId(),
							work.getIdInString()
					);
					for (DirectoryProvider provider : providers) {
						queueWithFlatDPs.add( new LuceneWorker.WorkWithPayload( work, provider ) );
					}
				}
				else if ( OptimizeLuceneWork.class.isAssignableFrom( work.getClass() ) ) {
					DirectoryProvider[] providers = shardingStrategy.getDirectoryProvidersForAllShards();
					for (DirectoryProvider provider : providers) {
						queueWithFlatDPs.add( new LuceneWorker.WorkWithPayload( work, provider ) );
					}
				}
				else {
					throw new AssertionFailure( "Unknown work type: " + work.getClass() );
				}
			}
			deadlockFreeQueue( queueWithFlatDPs, searchFactoryImplementor );
			checkForBatchIndexing(workspace);		
			for ( LuceneWorker.WorkWithPayload luceneWork : queueWithFlatDPs ) {
				worker.performWork( luceneWork );
			}
		}
		finally {
			workspace.clean();
			queue.clear();
		}
	}

	private void checkForBatchIndexing(Workspace workspace) {
		for ( LuceneWork luceneWork : queue ) {
			// if there is at least a single batch index job we put the work space into batch indexing mode.
			if( luceneWork.isBatch() ){
				log.trace( "Setting batch indexing mode." );
				workspace.setBatch( true );
				break;
			}
		}
	}

	/**
	 * one must lock the directory providers in the exact same order to avoid
	 * dead lock between concurrent threads or processes
	 * To achieve that, the work will be done per directory provider
	 */
	private void deadlockFreeQueue(List<LuceneWorker.WorkWithPayload> queue, final SearchFactoryImplementor searchFactoryImplementor) {
		Collections.sort( queue, new Comparator<LuceneWorker.WorkWithPayload>() {
			public int compare(LuceneWorker.WorkWithPayload o1, LuceneWorker.WorkWithPayload o2) {
				long h1 = getWorkHashCode( o1, searchFactoryImplementor );
				long h2 = getWorkHashCode( o2, searchFactoryImplementor );
				return h1 < h2 ?
						-1 :
						h1 == h2 ?
							0 :
							1;
			}
		} );
	}

	private long getWorkHashCode(LuceneWorker.WorkWithPayload luceneWork, SearchFactoryImplementor searchFactoryImplementor) {
		DirectoryProvider provider = luceneWork.getProvider();
		int h = provider.getClass().hashCode();
		h = 31 * h + provider.hashCode();
		long extendedHash = h; //to be sure extendedHash + 1 < extendedHash + 2 is always true
		if ( luceneWork.getWork() instanceof AddLuceneWork ) extendedHash+=1; //addwork after deleteWork
		if ( luceneWork.getWork() instanceof OptimizeLuceneWork ) extendedHash+=2; //optimize after everything
		return extendedHash;
	}
}
