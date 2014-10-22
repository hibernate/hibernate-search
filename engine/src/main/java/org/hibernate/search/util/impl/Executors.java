/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl;

import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Helper class to create threads;
 * these threads are grouped and named to be identified in a profiler.
 *
 * @author Sanne Grinovero
 */
public final class Executors {

	public static final int QUEUE_MAX_LENGTH = 1000;
	private static final String THREAD_GROUP_PREFIX = "Hibernate Search: ";

	private static final Log log = LoggerFactory.make();

	private Executors() {
		//now allowed
	}

	/**
	 * Creates a new fixed size ThreadPoolExecutor.
	 * It's using a blockingqueue of maximum 1000 elements and the rejection
	 * policy is set to CallerRunsPolicy for the case the queue is full.
	 * These settings are required to cap the queue, to make sure the
	 * timeouts are reasonable for most jobs.
	 *
	 * @param threads the number of threads
	 * @param groupname a label to identify the threadpool; useful for profiling.
	 * @return the new ExecutorService
	 */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String groupname) {
		return newFixedThreadPool( threads, groupname, QUEUE_MAX_LENGTH );
	}

	/**
	 * Creates a new fixed size ThreadPoolExecutor
	 *
	 * @param threads the number of threads
	 * @param groupname a label to identify the threadpool; useful for profiling.
	 * @param queueSize the size of the queue to store Runnables when all threads are busy
	 * @return the new ExecutorService
	 */
	public static ThreadPoolExecutor newFixedThreadPool(int threads, String groupname, int queueSize) {
		return new ThreadPoolExecutor(
				threads,
				threads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>( queueSize ),
				new SearchThreadFactory( groupname ),
				new BlockPolicy()
		);
	}

	/**
	 * Creates an executor for recurring tasks
	 *
	 * @param groupname a label to identify the threadpool; useful for profiling.
	 * @return instance of {@link ScheduledThreadPoolExecutor}
	 */
	public static ScheduledExecutorService newScheduledThreadPool(String groupname) {
		return new ScheduledThreadPoolExecutor( 1, new SearchThreadFactory( groupname ) );
	}

	/**
	 * Creates a dynamically scalable threadpool having an upper bound of threads and queue size
	 * which ultimately falls back to a CallerRunsPolicy.
	 * @param threadsMin initial and minimum threadpool size
	 * @param threadsMax maximumx threadpool size
	 * @param groupname used to assign nice names to the threads to help diagnostics and tuning
	 * @param queueSize maximum size of the blocking queue holding the work
	 * @return the new Executor instance
	 */
	public static ThreadPoolExecutor newScalableThreadPool(int threadsMin, int threadsMax, String groupname, int queueSize) {
		return new ThreadPoolExecutor(
				threadsMin,
				threadsMax,
				30,
				TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>( queueSize ),
				new SearchThreadFactory( groupname ),
				new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy()
				);
	}

	/**
	 * The thread factory, used to customize thread names
	 */
	private static class SearchThreadFactory implements ThreadFactory {

		final ThreadGroup group;
		final AtomicInteger threadNumber = new AtomicInteger( 1 );
		final String namePrefix;

		SearchThreadFactory(String groupname) {
			SecurityManager s = System.getSecurityManager();
			group = ( s != null ) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
			namePrefix = THREAD_GROUP_PREFIX + groupname + "-";
		}

		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread( group, r, namePrefix + threadNumber.getAndIncrement(), 0 );
			return t;
		}

	}

	/**
	 * A handler for rejected tasks that will have the caller block until space is available.
	 */
	public static class BlockPolicy implements RejectedExecutionHandler {

		/**
		 * Creates a <tt>BlockPolicy</tt>.
		 */
		public BlockPolicy() {
		}

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
