/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.impl.lucene.works.IndexUpdateVisitor;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkExecutor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.spi.DirectoryBasedIndexManager;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Collects all resources needed to apply changes to one index,
 * and are reused across several WorkQueues.
 *
 * @author Sanne Grinovero
 */
public final class LuceneBackendResources {

	private final AbstractWorkspaceImpl workspace;
	private final ErrorHandler errorHandler;
	private final String indexName;
	private final IndexManager indexManager;
	private final LazyExecutorHolder asynchExecutor;

	/**
	 * Lazily initialized; no need for locking as multiple instances can
	 * simply be discarded.
	 */
	private volatile IndexWorkVisitor<Void, LuceneWorkExecutor> workVisitor;

	/**
	 * Externally exposed Read/Write locks used by FSMasterDirectoryProvider
	 * to be able to make copies of a locked (immutable) index.
	 * TODO: explore if that could use a better snapshotting technique
	 */
	private final ReadLock readLock;
	private final WriteLock writeLock;

	LuceneBackendResources(WorkerBuildContext context, DirectoryBasedIndexManager indexManager, Properties props, AbstractWorkspaceImpl workspace) {
		this.indexName = indexManager.getIndexName();
		this.indexManager = indexManager;
		this.errorHandler = context.getErrorHandler();
		this.workspace = workspace;
		final ReentrantReadWriteLock indexReadWriteLock = new ReentrantReadWriteLock();
		this.readLock = indexReadWriteLock.readLock();
		this.writeLock = indexReadWriteLock.writeLock();
		final int maxQueueLength = PropertiesParseHelper.extractMaxQueueSize( indexName, props );
		this.asynchExecutor = new LazyExecutorHolder( maxQueueLength, indexName, "Index updates queue processor for index " + indexName );
	}

	private LuceneBackendResources(LuceneBackendResources previous) {
		this.indexManager = previous.indexManager;
		this.indexName = previous.indexName;
		this.errorHandler = previous.errorHandler;
		this.workspace = previous.workspace;
		this.readLock = previous.readLock;
		this.writeLock = previous.writeLock;
		this.asynchExecutor = previous.asynchExecutor;
	}

	public int getMaxQueueLength() {
		return asynchExecutor.getMaxQueueLength();
	}

	public String getIndexName() {
		return indexName;
	}

	public IndexManager getIndexManager() {
		return indexManager;
	}

	public IndexWorkVisitor<Void, LuceneWorkExecutor> getWorkVisitor() {
		if ( workVisitor == null ) {
			workVisitor = new IndexUpdateVisitor( workspace );
		}
		return workVisitor;
	}

	public AbstractWorkspaceImpl getWorkspace() {
		return workspace;
	}

	public void shutdown() {
		//need to close them in this specific order:
		try {
			asynchExecutor.flushCloseExecutor();
		}
		finally {
			workspace.shutDownNow();
		}
	}

	public ErrorHandler getErrorHandler() {
		return errorHandler;
	}

	public Lock getParallelModificationLock() {
		return readLock;
	}

	public Lock getExclusiveModificationLock() {
		return writeLock;
	}

	/**
	 * Creates a replacement for this same LuceneBackendResources:
	 * reuses the existing locks and executors (which can't be reconfigured on the fly),
	 * reuses the same Workspace and ErrorHandler, but will use a new LuceneWorkVisitor.
	 * The LuceneWorkVisitor contains the strategies we use to apply update operations on the index,
	 * and we might need to change them after the backend is started.
	 *
	 * @return the new LuceneBackendResources to replace this one.
	 */
	public LuceneBackendResources onTheFlyRebuild() {
		return new LuceneBackendResources( this );
	}

	public void flushAndReleaseResources() {
		asynchExecutor.flushCloseExecutor();
		workspace.getCommitPolicy().onClose();
		workspace.closeIndexWriter();
	}

	public void submitToAsyncIndexingExecutor(LuceneBackendQueueTask luceneBackendQueueProcessor) {
		asynchExecutor.submitTask( luceneBackendQueueProcessor );
	}
}
