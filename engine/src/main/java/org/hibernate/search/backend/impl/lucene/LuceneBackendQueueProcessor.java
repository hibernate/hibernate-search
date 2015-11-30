/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

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
public class LuceneBackendQueueProcessor implements BackendQueueProcessor {

	private static final Log log = LoggerFactory.make();

	private volatile LuceneBackendResources resources;
	private boolean sync;
	private AbstractWorkspaceImpl workspaceOverride;
	private LuceneBackendTaskStreamer streamWorker;
	private WorkProcessor workProcessor;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, DirectoryBasedIndexManager indexManager) {
		sync = BackendFactory.isConfiguredAsSync( props );
		if ( workspaceOverride == null ) {
			workspaceOverride = WorkspaceFactory.createWorkspace(
					indexManager, context, props
			);
		}
		resources = new LuceneBackendResources( context, indexManager, props, workspaceOverride );
		streamWorker = new LuceneBackendTaskStreamer( resources );
		String indexName = indexManager.getIndexName();
		if ( sync ) {
			final SyncWorkProcessor batchSyncProcessor = new SyncWorkProcessor( resources, indexName );
			batchSyncProcessor.start();
			log.luceneBackendInitializedSynchronously( indexName );
			workProcessor = batchSyncProcessor;
		}
		else {
			workProcessor = new AsyncWorkProcessor( resources );
			log.luceneBackendInitializedAsynchronously( indexName );
		}
	}

	@Override
	public void close() {
		//The order is important! The work processor uses the resources.
		workProcessor.shutdown();
		resources.shutdown();
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		if ( singleOperation == null ) {
			throw new IllegalArgumentException( "singleOperation should not be null" );
		}
		streamWorker.doWork( singleOperation, monitor );
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		if ( workList == null ) {
			throw new IllegalArgumentException( "workList should not be null" );
		}
		workProcessor.submit( workList, monitor );
	}

	@Override
	public Lock getExclusiveWriteLock() {
		return resources.getExclusiveModificationLock();
	}

	public LuceneBackendResources getIndexResources() {
		return resources;
	}

	/**
	 * If invoked before {@link #initialize(Properties, WorkerBuildContext, DirectoryBasedIndexManager)}
	 * it can set a customized Workspace instance to be used by this backend.
	 *
	 * @param workspace the new workspace
	 */
	public void setCustomWorkspace(AbstractWorkspaceImpl workspace) {
		this.workspaceOverride = workspace;
	}

	@Override
	public void indexMappingChanged() {
		resources = resources.onTheFlyRebuild();
		workProcessor.updateResources( resources );
	}

}
