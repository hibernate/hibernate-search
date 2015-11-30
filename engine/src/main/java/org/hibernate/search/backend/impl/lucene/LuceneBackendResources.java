/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
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
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Collects all resources needed to apply changes to one index,
 * and are reused across several WorkQueues.
 *
 * @author Sanne Grinovero
 */
public final class LuceneBackendResources {

	private static final Log log = LoggerFactory.make();

	private volatile IndexWorkVisitor<Void, LuceneWorkExecutor> workVisitor;
	private final AbstractWorkspaceImpl workspace;
	private final ErrorHandler errorHandler;
	private final int maxQueueLength;
	private final String indexName;

	private final ReadLock readLock;
	private final WriteLock writeLock;

	private volatile ExecutorService asyncIndexingExecutor;

	LuceneBackendResources(WorkerBuildContext context, DirectoryBasedIndexManager indexManager, Properties props, AbstractWorkspaceImpl workspace) {
		this.indexName = indexManager.getIndexName();
		this.errorHandler = context.getErrorHandler();
		this.workspace = workspace;
		this.maxQueueLength = PropertiesParseHelper.extractMaxQueueSize( indexName, props );
		ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();
	}

	private LuceneBackendResources(LuceneBackendResources previous) {
		this.indexName = previous.indexName;
		this.errorHandler = previous.errorHandler;
		this.workspace = previous.workspace;
		this.maxQueueLength = previous.maxQueueLength;
		this.asyncIndexingExecutor = previous.asyncIndexingExecutor;
		this.readLock = previous.readLock;
		this.writeLock = previous.writeLock;
	}

	public ExecutorService getAsynchIndexingExecutor() {
		ExecutorService executor = asyncIndexingExecutor;
		if ( executor != null ) {
			return executor;
		}
		else {
			return getAsynchIndexingExecutorSynchronized();
		}
	}

	private synchronized ExecutorService getAsynchIndexingExecutorSynchronized() {
		ExecutorService executor = asyncIndexingExecutor;
		if ( executor != null ) {
			return executor;
		}
		else {
			this.asyncIndexingExecutor = Executors.newFixedThreadPool( 1, "Index updates queue processor for index " + indexName, maxQueueLength );
			return this.asyncIndexingExecutor;
		}
	}

	public int getMaxQueueLength() {
		return maxQueueLength;
	}

	public String getIndexName() {
		return indexName;
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
			flushCloseExecutor();
		}
		finally {
			workspace.shutDownNow();
		}
	}

	private void flushCloseExecutor() {
		if ( asyncIndexingExecutor == null ) {
			return;
		}
		asyncIndexingExecutor.shutdown();
		try {
			asyncIndexingExecutor.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForIndexActivity( e );
		}
		if ( ! asyncIndexingExecutor.isTerminated() ) {
			log.unableToShutdownAsynchronousIndexingByTimeout( indexName );
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

}
