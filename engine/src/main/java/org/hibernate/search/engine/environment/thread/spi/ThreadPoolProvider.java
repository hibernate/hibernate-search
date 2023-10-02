/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.environment.thread.spi;

import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public interface ThreadPoolProvider {

	/**
	 * @return The underlying thread provider.
	 */
	ThreadProvider threadProvider();

	/**
	 * Creates a new fixed size {@link ThreadPoolExecutor}.
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
	ThreadPoolExecutor newFixedThreadPool(int threads, String threadNamePrefix);

	/**
	 * Creates a new fixed size {@link ThreadPoolExecutor}.
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
	ThreadPoolExecutor newFixedThreadPool(int threads, String threadNamePrefix, int queueSize);

	/**
	 * Creates a new fixed size {@link ScheduledExecutorService}.
	 * <p>
	 * The queue size is not capped, so users should take care of checking they submit a reasonable amount of tasks.
	 *
	 * @param threads the number of threads
	 * @param threadNamePrefix a label to identify the threads; useful for profiling.
	 * @return the new ExecutorService
	 */
	ScheduledExecutorService newScheduledExecutor(int threads, String threadNamePrefix);

	/**
	 *  @return {@code true} if the executors returned by {@link #newScheduledExecutor} may block when submitting a task;
	 *  {@code false} if they never block (e.g. they throw an {@link RejectedExecutionException}).
	 */
	boolean isScheduledExecutorBlocking();

}
