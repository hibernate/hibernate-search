/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * A task that can be scheduled for a run and is guaranteed to never run concurrently,
 * regardless of the thread pool it's submitted to.
 */
public final class SingletonTask {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;
	private final Runnable runnable;
	private final Scheduler scheduler;
	private final FailureHandler failureHandler;

	private final AtomicReference<Status> status = new AtomicReference<>( Status.IDLE );
	private volatile boolean needsRun;
	private volatile Future<?> nextExecutionFuture;
	private volatile CompletableFuture<?> completionFuture;

	public SingletonTask(String name, Worker worker, Scheduler scheduler, FailureHandler failureHandler) {
		this.name = name;
		this.runnable = new RunnableWrapper( worker );
		this.scheduler = scheduler;
		this.failureHandler = failureHandler;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ "status=" + status
				+ ", needsRun=" + needsRun
				+ ", nextExecutionFuture=" + nextExecutionFuture
				+ "]";
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
			// the worker detects there is more work to do (see uses of needsRun).
			return;
		}

		log.tracef( "Scheduling task '%s'.", name );

		/*
		 * Our thread successfully switched the status:
		 * the task wasn't in progress, and we're now responsible for scheduling it.
		 */
		try {
			if ( completionFuture == null ) {
				/*
				 * The task was previously not running:
				 * we need to create a new future for the completion of the task.
				 * This is not executed when re-scheduling the task in ReschedulingRunnable.
				 */
				completionFuture = new CompletableFuture<>();
			}
			nextExecutionFuture = scheduler.schedule( runnable );
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
	public CompletableFuture<?> completion() {
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
		cancelIfNotNull( nextExecutionFuture );
		nextExecutionFuture = null;

		cancelIfNotNull( completionFuture );
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
		 *
		 * @return A future completing when the executor is allowed to call this method again.
		 */
		CompletableFuture<?> work();

		/**
		 * Executes any outstanding operation, or schedule their execution.
		 * <p>
		 * Called when the worker is not expected to work in the foreseeable future.
		 */
		void complete();
	}

	public interface Scheduler {

		Future<?> schedule(Runnable runnable);

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
		private final BiFunction<Object, Throwable, Object> workFinishedHandler = Futures.handler( this::onWorkFinished );

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
				log.tracef( "Running task '%s'", name );
				worker.work().handle( workFinishedHandler );
			}
			catch (Throwable e) {
				onWorkFinished( null, e );
			}
		}

		private Void onWorkFinished(Object ignored, Throwable throwable) {
			if ( throwable != null ) {
				handleUnexpectedFailure( throwable, "Executing task '" + name + "'" );
			}

			try {
				afterRun();
			}
			catch (Throwable e) {
				handleUnexpectedFailure( e, "Handling post-execution in task '" + name + "'" );
			}

			return null;
		}

		private void afterRun() {
			if ( !needsRun ) {
				// We're done running this task.

				// First, tell the worker that we're done.
				try {
					log.tracef( "Completed task '%s'", name );
					worker.complete();
				}
				catch (Throwable e) {
					handleUnexpectedFailure( e, "Calling worker.complete() in task '" + name + "'" );
				}

				// Tell callers of getCompletion()
				CompletableFuture<?> justFinishedExecutionFuture = completionFuture;
				completionFuture = null;
				justFinishedExecutionFuture.complete( null );
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

		// This will only be called if there is a bug in the task, but we don't want to fail silently.
		private void handleUnexpectedFailure(Throwable throwable, String failingOperation) {
			FailureContext.Builder contextBuilder = FailureContext.builder();
			contextBuilder.throwable( throwable );
			contextBuilder.failingOperation( failingOperation );
			failureHandler.handle( contextBuilder.build() );
		}
	}
}
