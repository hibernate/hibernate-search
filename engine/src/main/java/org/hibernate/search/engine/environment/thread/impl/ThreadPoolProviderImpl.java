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

import org.hibernate.search.engine.environment.bean.BeanHolder;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.environment.thread.spi.ThreadProvider;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Helper to create threads and executors.
 *
 * @author Sanne Grinovero
 */
public class ThreadPoolProviderImpl implements ThreadPoolProvider {

	private static final int QUEUE_MAX_LENGTH = 1000;

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BeanHolder<? extends ThreadProvider> threadProviderHolder;

	public ThreadPoolProviderImpl(BeanHolder<? extends ThreadProvider> threadProviderHolder) {
		this.threadProviderHolder = threadProviderHolder;
	}

	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BeanHolder::close, threadProviderHolder );
		}
	}

	@Override
	public ThreadProvider threadProvider() {
		return threadProviderHolder.get();
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
				threadProviderHolder.get().createThreadFactory( threadNamePrefix ),
				new BlockPolicy()
		);
	}

	@Override
	public ScheduledExecutorService newScheduledExecutor(int threads, String threadNamePrefix) {
		ScheduledThreadPoolExecutor result = new ScheduledThreadPoolExecutor(
				threads,
				threadProviderHolder.get().createThreadFactory( threadNamePrefix ),
				new BlockPolicy()
		);
		// Prevents cancelled tasks from piling up in the execution queue.
		// This means cancellation will be in O(log(n) instead of O(1),
		// but it's preferable if we have lots of cancelled tasks,
		// which is the case when we use the thread pool for timeouts in particular.
		result.setRemoveOnCancelPolicy( true );
		return result;
	}

	@Override
	public boolean isScheduledExecutorBlocking() {
		// a ScheduledExecutorService returned by this provider is using an unlimited BlockingQueue underneath.
		// Hence, it'll accept all the tasks and will sooner produce OOM rather than block.
		return false;
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
