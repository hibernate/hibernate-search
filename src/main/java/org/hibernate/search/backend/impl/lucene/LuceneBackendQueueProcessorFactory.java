//$Id$
package org.hibernate.search.backend.impl.lucene;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.hibernate.search.backend.BackendQueueProcessorFactory;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.BatchedQueueingProcessor;
import org.hibernate.search.engine.SearchFactoryImplementor;
import org.hibernate.search.store.DirectoryProvider;

/**
 * This will actually contain the Workspace and LuceneWork visitor implementation,
 * reused per-DirectoryProvider.
 * Both Workspace(s) and LuceneWorkVisitor(s) lifecycle are linked to the backend
 * lifecycle (reused and shared by all transactions).
 * The LuceneWorkVisitor(s) are stateless, the Workspace(s) are threadsafe.
 * 
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class LuceneBackendQueueProcessorFactory implements BackendQueueProcessorFactory {

	private SearchFactoryImplementor searchFactoryImp;
	
	/**
	 * Contains the Workspace and LuceneWork visitor implementation,
	 * reused per-DirectoryProvider.
	 * Both Workspace(s) and LuceneWorkVisitor(s) lifecycle are linked to the backend
	 * lifecycle (reused and shared by all transactions);
	 * the LuceneWorkVisitor(s) are stateless, the Workspace(s) are threadsafe.
	 */
	private final Map<DirectoryProvider,PerDPResources> resourcesMap = new HashMap<DirectoryProvider,PerDPResources>();

	/**
	 * copy of BatchedQueueingProcessor.sync
	 */
	private boolean sync;

	public void initialize(Properties props, SearchFactoryImplementor searchFactoryImplementor) {
		this.searchFactoryImp = searchFactoryImplementor;
		this.sync = BatchedQueueingProcessor.isConfiguredAsSync( props );
		for (DirectoryProvider dp : searchFactoryImplementor.getDirectoryProviders() ) {
			PerDPResources resources = new PerDPResources( searchFactoryImplementor, dp );
			resourcesMap.put( dp, resources );
		}
	}

	public Runnable getProcessor(List<LuceneWork> queue) {
		return new LuceneBackendQueueProcessor( queue, searchFactoryImp, resourcesMap, sync );
	}

	public void close() {
		// needs to stop all used ThreadPools
		for (PerDPResources res : resourcesMap.values() ) {
			ExecutorService executor = res.getExecutor();
			executor.shutdown();
		}
	}
	
}
