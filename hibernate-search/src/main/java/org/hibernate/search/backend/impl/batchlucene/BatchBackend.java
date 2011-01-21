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

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Implementations of this interface are not drop-in replacements for the standard BackendQueueProcessorFactory,
 * but are meant to be used only during batch processing.
 * The order of LuceneWork(s) processed is not guaranteed as the queue is consumed by several
 * concurrent workers.
 *
 * @author Sanne Grinovero
 */
public interface BatchBackend {

	/**
	 * Used at startup, called once as first method.
	 *
	 * @param props all configuration properties
	 * @param monitor the indexing progress monitor
	 * @param context the build context for the workers
	 */
	void initialize(Properties props, MassIndexerProgressMonitor monitor, WorkerBuildContext context);

	/**
	 * Enqueues one work to be processed asynchronously
	 *
	 * @param work
	 *
	 * @throws InterruptedException if the current thread is interrupted while
	 *                              waiting for the work queue to have enough space.
	 */
	void enqueueAsyncWork(LuceneWork work) throws InterruptedException;

	/**
	 * Does one work in sync
	 *
	 * @param work
	 *
	 * @throws InterruptedException
	 */
	void doWorkInSync(LuceneWork work);

	/**
	 * Waits until all work is done and terminates the executors.
	 * IndexWriter is not closed yet: work in sync can still be processed.
	 *
	 * @throws InterruptedException if the current thread is interrupted
	 *                              while waiting for the enqueued tasks to be finished.
	 */
	void stopAndFlush(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * Used to shutdown and release resources.
	 * No other method should be used after this one.
	 */
	void close();
}
