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

import org.hibernate.search.batchindexing.impl.Executors;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.backend.BackendFactory;
import org.hibernate.search.backend.impl.lucene.works.LuceneWorkVisitor;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.indexes.impl.CommonPropertiesParse;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * Collects all resources needed to apply changes to one index,
 * and are reused across several WorkQueues.
 *
 * @author Sanne Grinovero
 */
public final class LuceneBackendResources {
	
	private static final Log log = LoggerFactory.make();
	
	private final LuceneWorkVisitor visitor;
	private final AbstractWorkspaceImpl workspace;
	private final ErrorHandler errorHandler;
	private final ExecutorService queueingExecutor;
	private final ExecutorService workersExecutor;
	private final int maxQueueLength;
	private final String indexName;

	private final ReadLock readLock;
	private final WriteLock writeLock;
	
	LuceneBackendResources(WorkerBuildContext context, DirectoryBasedIndexManager indexManager, Properties props, AbstractWorkspaceImpl workspace) {
		this.indexName = indexManager.getIndexName();
		this.errorHandler = context.getErrorHandler();
		this.workspace = workspace;
		this.visitor = new LuceneWorkVisitor( workspace );
		this.maxQueueLength = CommonPropertiesParse.extractMaxQueueSize( indexName, props );
		this.queueingExecutor = Executors.newFixedThreadPool( 1, "Index updates queue processor for index " + indexName, maxQueueLength );
		this.workersExecutor = BackendFactory.buildWorkersExecutor( props, indexName );
		ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
		readLock = readWriteLock.readLock();
		writeLock = readWriteLock.writeLock();
	}

	private LuceneBackendResources(LuceneBackendResources previous) {
		this.indexName = previous.indexName;
		this.errorHandler = previous.errorHandler;
		this.workspace = previous.workspace;
		this.visitor = new LuceneWorkVisitor( workspace );
		this.maxQueueLength = previous.maxQueueLength;
		this.queueingExecutor = previous.queueingExecutor;
		this.workersExecutor = previous.workersExecutor;
		this.readLock = previous.readLock;
		this.writeLock = previous.writeLock;
	}

	public ExecutorService getQueueingExecutor() {
		return queueingExecutor;
	}

	public ExecutorService getWorkersExecutor() {
		return workersExecutor;
	}

	public int getMaxQueueLength() {
		return maxQueueLength;
	}

	public String getIndexName() {
		return indexName;
	}

	public LuceneWorkVisitor getVisitor() {
		return visitor;
	}

	public AbstractWorkspaceImpl getWorkspace() {
		return workspace;
	}

	public void shutdown() {
		//need to close them in this specific order:
		try {
			flushCloseExecutor( queueingExecutor );
			flushCloseExecutor( workersExecutor );
		}
		finally {
			workspace.shutDownNow();
		}
	}

	private void flushCloseExecutor(ExecutorService executor) {
		executor.shutdown();
		try {
			executor.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
		}
		catch ( InterruptedException e ) {
			log.interruptedWhileWaitingForIndexActivity( e );
			Thread.currentThread().interrupt();
		}
		if ( ! executor.isTerminated() ) {
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
