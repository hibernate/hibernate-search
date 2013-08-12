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
package org.hibernate.search.backend.impl.lucene;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.SearchException;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
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
	}

	@Override
	public void close() {
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
		LuceneBackendQueueTask luceneBackendQueueProcessor = new LuceneBackendQueueTask(
				workList,
				resources,
				monitor
		);
		if ( sync ) {
			Future<?> future = resources.getQueueingExecutor().submit( luceneBackendQueueProcessor );
			try {
				future.get();
			}
			catch (InterruptedException e) {
				log.interruptedWhileWaitingForIndexActivity( e );
				Thread.currentThread().interrupt();
			}
			catch (ExecutionException e) {
				throw new SearchException( "Error applying updates to the Lucene index", e.getCause() );
			}
		}
		else {
			resources.getQueueingExecutor().execute( luceneBackendQueueProcessor );
		}
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
	}

}
