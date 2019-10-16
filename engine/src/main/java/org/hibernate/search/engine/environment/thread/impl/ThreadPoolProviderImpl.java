/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.environment.thread.impl;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Helper to create threads and executors.
 *
 * @author Sanne Grinovero
 */
public final class ThreadPoolProviderImpl implements ThreadPoolProvider {

	private static final int QUEUE_MAX_LENGTH = 1000;

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ThreadProvider threadProvider;

	public ThreadPoolProviderImpl(ThreadProvider threadProvider) {
		this.threadProvider = threadProvider;
	}

	@Override
	public ThreadProvider getThreadProvider() {
		return threadProvider;
	}

	@Override
	public ThreadPoolExecutor newFixedThreadPool(int threads, String threadNamePrefix) {
		return newFixedThreadPool( threads, threadNamePrefix, QUEUE_MAX_LENGTH );
	}

	@Override
	public ThreadPoolExecutor newFixedThreadPool(int threads, String threadNamePrefix, int queueSize) {
		return new ThreadPoolExecutor(
				threads,
				threads,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>( queueSize ),
				threadProvider.createThreadFactory( threadNamePrefix ),
				new BlockPolicy()
		);
	}

	@Override
	public ScheduledExecutorService newScheduledThreadPool(String threadNamePrefix) {
		return new ScheduledThreadPoolExecutor( 1, threadProvider.createThreadFactory( threadNamePrefix ) );
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
