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

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;

import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * Apply the operations to Lucene directories.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 * @author Sanne Grinovero
 */
final class LuceneBackendQueueTask implements Runnable {

	private static final Log log = LoggerFactory.make();

	private final Lock modificationLock;
	private final LuceneBackendResources resources;
	private final List<LuceneWork> queue;
	private final IndexingMonitor monitor;

	LuceneBackendQueueTask(List<LuceneWork> queue, LuceneBackendResources resources, IndexingMonitor monitor) {
		this.queue = queue;
		this.resources = resources;
		this.monitor = monitor;
		this.modificationLock = resources.getParallelModificationLock();
	}

	@Override
	public void run() {
		modificationLock.lock();
		try {
			applyUpdates();
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForIndexActivity( e );
			Thread.currentThread().interrupt();
			handleException( e );
		}
		catch (Exception e) {
			log.backendError( e );
			handleException( e );
		}
		finally {
			modificationLock.unlock();
		}
	}

	private void handleException(Exception e) {
		ErrorContextBuilder builder = new ErrorContextBuilder();
		builder.allWorkToBeDone( queue );
		builder.errorThatOccurred( e );
		resources.getErrorHandler().handle( builder.createErrorContext() );
	}

	/**
	 * Applies all modifications to the index in parallel using the workers executor
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	private void applyUpdates() throws InterruptedException, ExecutionException {
		AbstractWorkspaceImpl workspace = resources.getWorkspace();

		ErrorContextBuilder errorContextBuilder = new ErrorContextBuilder();
		errorContextBuilder.allWorkToBeDone( queue );

		IndexWriter indexWriter = workspace.getIndexWriter( errorContextBuilder );
		if ( indexWriter == null ) {
			log.cannotOpenIndexWriterCausePreviousError();
			return;
		}
		LinkedList<LuceneWork> failedUpdates = null;
		try {
			if ( queue.size() == 1 ) {
				failedUpdates = runSingleTask( queue.get( 0 ), resources, indexWriter, monitor, errorContextBuilder );
			}
			else {
				failedUpdates = runMultipleTasks( queue, resources, indexWriter, monitor, errorContextBuilder );
			}
			if ( failedUpdates != null ) {
				errorContextBuilder.addAllWorkThatFailed( failedUpdates );
				resources.getErrorHandler().handle( errorContextBuilder.createErrorContext() );
			}
			else {
				workspace.optimizerPhase();
			}
		}
		finally {
			workspace.afterTransactionApplied( failedUpdates != null, false );
		}
	}

	/**
	 * Applies each modification in parallel using the backend workers pool
	 * @throws InterruptedException
	 */
	private LinkedList<LuceneWork> runMultipleTasks(final List<LuceneWork> queue, final LuceneBackendResources resources,
			final IndexWriter indexWriter, final IndexingMonitor monitor, final ErrorContextBuilder errorContextBuilder) throws InterruptedException {
		final int queueSize = queue.size();
		final ExecutorService executor = resources.getWorkersExecutor();
		final Future[] submittedTasks = new Future[ queueSize ];
		LinkedList<LuceneWork> failedUpdates = null;
		for ( int i = 0; i < queueSize; i++ ) {
			SingleTaskRunnable task = new SingleTaskRunnable( queue.get( i ), resources, indexWriter, monitor );
			submittedTasks[i] = executor.submit( task );
		}
		// now wait for all tasks being completed before releasing our lock
		// (this thread waits even in async backend mode)
		for ( int i = 0; i < queueSize; i++ ) {
			Future task = submittedTasks[i];
			try {
				task.get();
				errorContextBuilder.workCompleted( queue.get( i ) );
			}
			catch (ExecutionException e) {
				if ( failedUpdates == null ) {
					failedUpdates = new LinkedList<LuceneWork>();
				}
				failedUpdates.add( queue.get( i ) );
				errorContextBuilder.errorThatOccurred( e.getCause() );
			}
		}
		return failedUpdates;
	}

	/**
	 * Applies a single modification using the caller's thread to avoid pointless context
	 * switching.
	 */
	private LinkedList<LuceneWork> runSingleTask(final LuceneWork luceneWork, final LuceneBackendResources resources,
			final IndexWriter indexWriter, final IndexingMonitor monitor, final ErrorContextBuilder errorContextBuilder) {
		final SingleTaskRunnable task = new SingleTaskRunnable( queue.get( 0 ), resources, indexWriter, monitor );
		try {
			task.run();
			return null;
		}
		catch (RuntimeException re) {
			errorContextBuilder.errorThatOccurred( re );
			LinkedList<LuceneWork> failedUpdates = new LinkedList<LuceneWork>();
			failedUpdates.add( luceneWork );
			return failedUpdates;
		}
	}

}
