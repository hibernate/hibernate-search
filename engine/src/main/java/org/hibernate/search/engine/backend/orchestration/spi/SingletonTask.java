/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;

/**
 * A task that can be scheduled for a run and is guaranteed to never run concurrently,
 * regardless of the thread pool it's submitted to.
 */
public final class SingletonTask {

	private final String name;
	private final Runnable runnable;
	private final ExecutorService executorService;
	private final ScheduledExecutorService scheduledExecutorService;
	private final FailureHandler failureHandler;

	private final AtomicReference<Status> status = new AtomicReference<>( Status.IDLE );
	private volatile boolean needsRun;
	private volatile Future<?> nextExecutionFuture;
	private volatile ScheduledFuture<?> delayedExecutionFuture;
	private volatile CompletableFuture<?> completionFuture;

	public SingletonTask(String name, Worker worker,
			ExecutorService executorService, ScheduledExecutorService scheduledExecutorService,
			FailureHandler failureHandler) {
		this.name = name;
		this.runnable = new RunnableWrapper( worker );
		this.executorService = executorService;
		this.scheduledExecutorService = scheduledExecutorService;
		this.failureHandler = failureHandler;
	}

	/**
	 * Ensures the task will run in the future.
	 * <ul>
	 * <li>If the task is neither scheduled nor running, this schedules the task for execution.</li>
	 * <li>If the task is already scheduled but not running, this does nothing: the task remains scheduled.</li>
	 * <li>If the task is running: the task execution continues and will trigger re-scheduling
	 * when it finishes.</li>
	 * </ul>
	 */
	public void ensureScheduled() {
		// Make sure the task will be re-scheduled if it is currently running.
		needsRun = true;
		if ( !status.compareAndSet( Status.IDLE, Status.SCHEDULED ) ) {
			// Already scheduled.
			// If the task hasn't started running yet, we're good.
			// If the task is running, it will re-schedule itself automatically if
			// the worker detects there is more work to do (see hasMoreWork()).
			return;
		}

		/*
		 * Our thread successfully switched the status:
		 * the task wasn't in progress, and we're now responsible for scheduling it.
		 */
		try {
			if ( delayedExecutionFuture != null ) {
				/*
				 * We scheduled processing at a later time.
				 * Since we're going to execute processing right now,
				 * we can cancel this scheduling.
				 */
				cancelDelayed();
			}
			if ( completionFuture == null ) {
				/*
				 * The task was previously not running:
				 * we need to create a new future for the completion of the task.
				 * This is not executed when re-scheduling the task in ReschedulingRunnable.
				 */
				completionFuture = new CompletableFuture<>();
			}
			nextExecutionFuture = executorService.submit( runnable );
		}
		catch (Throwable e) {
			/*
			 * Make sure a failure to schedule the task
			 * doesn't prevent the task from running ever again,
			 * and doesn't leave other threads waiting indefinitely.
			 */
			try {
				CompletableFuture<?> future = completionFuture;
				completionFuture = null;
				status.set( Status.IDLE );
				future.completeExceptionally( e );
			}
			catch (Throwable e2) {
				e.addSuppressed( e2 );
			}
			throw e;
		}
	}

	/**
	 * @return A future that completes when all works submitted to the executor so far are completely executed.
	 * Works submitted to the executor after entering this method may delay the wait.
	 */
	public CompletableFuture<?> getCompletion() {
		CompletableFuture<?> future = completionFuture;
		if ( future == null ) {
			// No execution in progress or scheduled.
			return CompletableFuture.completedFuture( null );
		}
		else {
			// Execution in progress or scheduled; the future will be completed when execution stops
			// without being re-scheduled.
			return future;
		}
	}

	/**
	 * Stop the task.
	 * <p>
	 * Callers must call this as the very last method on this object;
	 * any concurrent call may lead to unpredictable results.
	 */
	public void stop() {
		cancelDelayed();

		cancelIfNotNull( nextExecutionFuture );
		nextExecutionFuture = null;

		cancelIfNotNull( completionFuture );
		completionFuture = null;
	}

	private void cancelDelayed() {
		cancelIfNotNull( delayedExecutionFuture );
		delayedExecutionFuture = null;
	}

	private void cancelIfNotNull(Future<?> futureToCancel) {
		if ( futureToCancel != null ) {
			futureToCancel.cancel( false );
		}
	}

	public interface Worker {
		/**
		 * Executes a unit of work.
		 * <p>
		 * If there is no work to do, this shouldn't do anything.
		 */
		void work();

		/**
		 * Executes any outstanding operation if possible, or return an estimation of when they can be executed.
		 * <p>
		 * Called when the worker is not expected to work in the foreseeable future.
		 *
		 * @return {@code 0} if there is no outstanding operation, or a positive number of milliseconds
		 * if there are outstanding operations and {@link #complete()}
		 * must be called again that many milliseconds later.
		 */
		long complete();
	}

	private enum Status {
		IDLE,
		SCHEDULED
	}

	/**
	 * A wrapper for runnables that ensures:
	 * <ul>
	 *     <li>Failures are reported to the failure handler</li>
	 *     <li>The task is re-scheduled after it ran if necessary.</li>
	 * </ul>
	 */
	private class RunnableWrapper implements Runnable {
		private final Worker worker;

		public RunnableWrapper(Worker worker) {
			this.worker = worker;
		}

		@Override
		public void run() {
			// Reset "needsRun": we're going to run right now.
			// It might be set again while we're running, in which case we'll re-schedule a run.
			needsRun = false;
			nextExecutionFuture = null;
			try {
				worker.work();
			}
			catch (Throwable e) {
				// This will only happen if there is a bug in the task, but we don't want to fail silently.
				FailureContext.Builder contextBuilder = FailureContext.builder();
				contextBuilder.throwable( e );
				contextBuilder.failingOperation( "Executing task '" + name + "'" );
				failureHandler.handle( contextBuilder.build() );
			}
			finally {
				try {
					afterRun();
				}
				catch (Throwable e) {
					// This will only happen if there is a bug in this class, but we don't want to fail silently.
					FailureContext.Builder contextBuilder = FailureContext.builder();
					contextBuilder.throwable( e );
					contextBuilder.failingOperation( "Handling post-execution in task '" + name + "'" );
					failureHandler.handle( contextBuilder.build() );
				}
			}
		}

		private void afterRun() {
			if ( !needsRun ) {
				// We're done running this task.
				// First, tell the worker that we're done.
				long delay = 0;
				try {
					delay = worker.complete();
				}
				catch (Throwable e) {
					// This will only happen if there is a bug in the worker, but we don't want to fail silently
					FailureContext.Builder contextBuilder = FailureContext.builder();
					contextBuilder.throwable( e );
					contextBuilder.failingOperation( "Calling worker.complete() in task '" + name + "'" );
					failureHandler.handle( contextBuilder.build() );
				}

				if ( delay <= 0 ) {
					// The worker acknowledged that all outstanding operations (commit, ...) have completed.
					// Tell callers of getCompletion()
					CompletableFuture<?> justFinishedExecutionFuture = completionFuture;
					completionFuture = null;
					justFinishedExecutionFuture.complete( null );
				}
				else {
					// The worker requested that we wait because some outstanding operations (commit, ...)
					// need to be performed later.
					delayedExecutionFuture = scheduledExecutorService.schedule(
							SingletonTask.this::ensureScheduled, delay, TimeUnit.MILLISECONDS
					);
				}
			}

			// Allow this thread (or others) to run processing again.
			status.set( Status.IDLE );

			// A call to ensureScheduled() may have happened before we reset the status above,
			// in which case it did not schedule the task because it was running.
			// If there is still work to do, ensure the task is scheduled
			// and will ultimately take care of the remaining work.
			if ( needsRun ) {
				ensureScheduled();
			}
		}
	}
}
