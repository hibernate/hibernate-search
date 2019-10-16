/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Helper class to create threads;
 * these threads are grouped and named to be identified in a profiler.
 *
 * @author Sanne Grinovero
 */
public final class Executors {

	public static final int QUEUE_MAX_LENGTH = 1000;

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private Executors() {
		//not allowed
	}

	/**
	 * Creates a new fixed size ThreadPoolExecutor.
	 * <p>
	 * It's using a blocking queue of maximum 1000 elements and the rejection
	 * policy is set to block until the queue can accept the task.
	 * These settings are required to cap the queue, to make sure the
	 * timeouts are reasonable for most jobs.
	 *
	 * @param threads the number of threads
	 * @param threadNamePrefix a label to identify the threads; useful for profiling.
	 * @return the new ExecutorService
	 */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String threadNamePrefix) {
		return newFixedThreadPool( threads, threadNamePrefix, QUEUE_MAX_LENGTH );
	}

	/**
	 * Creates a new fixed size ThreadPoolExecutor.
	 * <p>
	 * It's using a blocking queue of maximum {@code queueSize} elements and the rejection
	 * policy is set to block until the queue can accept the task.
	 * These settings are required to cap the queue, to make sure the
	 * timeouts are reasonable for most jobs.
	 *
	 * @param threads the number of threads
	 * @param threadNamePrefix a label to identify the threads; useful for profiling.
	 * @param queueSize the size of the queue to store Runnables when all threads are busy
	 * @return the new ExecutorService
	 */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String threadNamePrefix, int queueSize) {
		return new ThreadPoolExecutor(
				threads,
				threads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>( queueSize ),
				new SearchThreadFactory( threadNamePrefix ),
				new BlockPolicy()
		);
	}

	/**
	 * Creates an executor for recurring tasks
	 *
	 * @param threadNamePrefix a label to identify the threads; useful for profiling.
	 * @return instance of {@link ScheduledThreadPoolExecutor}
	 */
	public static ScheduledExecutorService newScheduledThreadPool(String threadNamePrefix) {
		return new ScheduledThreadPoolExecutor( 1, new SearchThreadFactory( threadNamePrefix ) );
	}

	/**
	 * A handler for rejected tasks that will have the caller block until space is available.
	 */
	public static class BlockPolicy implements RejectedExecutionHandler {

		/**
		 * Puts the Runnable to the blocking queue, effectively blocking the delegating thread until space is available.
		 *
		 * @param r the runnable task requested to be executed
		 * @param e the executor attempting to execute this task
		 */
		@Override
		public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
			try {
				e.getQueue().put( r );
			}
			catch (InterruptedException e1) {
				log.interruptedWorkError( r );
				Thread.currentThread().interrupt();
			}
		}
	}

}
