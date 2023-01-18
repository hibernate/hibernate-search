/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.execution.spi;

import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface SimpleScheduledExecutor {

	/**
	 * Submits a {@link Runnable} task for execution and returns a {@link Future} representing that task.
	 *
	 * @param task the task to submit
	 * @return a {@link Future} representing pending completion of the task
	 *
	 * @throws RejectedExecutionException if the task cannot be scheduled for execution
	 */
	Future<?> submit(Runnable task);

	/**
	 * Submits a task that becomes enabled after the given delay.
	 *
	 * @param command the task to execute
	 * @param delay the time from now to delay execution
	 * @param unit the time unit of the delay parameter
	 * @return a {@link ScheduledFuture} representing pending completion of the submitted task.
	 *
	 * @throws RejectedExecutionException if the task cannot be  scheduled for execution
	 */
	ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit);

	/**
	 * Attempts to stop all actively executing tasks, halts the processing of waiting tasks.
	 */
	void shutdownNow();

}
