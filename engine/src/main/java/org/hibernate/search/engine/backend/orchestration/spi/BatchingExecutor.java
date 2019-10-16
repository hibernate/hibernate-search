/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.impl.Futures;

/**
 * An executor of works that accepts works from multiple threads, puts them in a queue,
 * and processes them in batches in a single background thread.
 * <p>
 * Useful when works can be merged together for optimization purposes (bulking in Elasticsearch),
 * or when they should never be executed in parallel (writes to a Lucene index).
 */
public final class BatchingExecutor<W extends BatchingExecutor.WorkSet<? super P>, P extends BatchingExecutor.WorkProcessor> {

	private final String name;

	private final P processor;
	private final FailureHandler failureHandler;
	private final int maxTasksPerBatch;

	private final BlockingQueue<W> workQueue;
	private final List<W> workBuffer;
	private final AtomicBoolean processingInProgress;

	private ExecutorService executorService;
	private volatile CompletableFuture<?> completionFuture;

	/**
	 * @param name The name of the executor thread (and of this executor when reporting errors)
	 * @param processor A task processor. May not be thread-safe.
	 * @param maxTasksPerBatch The maximum number of tasks to process in a single batch.
	 * Higher values mean more opportunity for the processor to optimize execution, but higher heap consumption.
	 * @param fair if {@code true} tasks are always submitted to the
	 * processor in FIFO order, if {@code false} tasks submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	public BatchingExecutor(String name,
			P processor, int maxTasksPerBatch, boolean fair,
			FailureHandler failureHandler) {
		this.name = name;
		this.processor = processor;
		this.failureHandler = failureHandler;
		this.maxTasksPerBatch = maxTasksPerBatch;
		workQueue = new ArrayBlockingQueue<>( maxTasksPerBatch, fair );
		workBuffer = new ArrayList<>( maxTasksPerBatch );
		processingInProgress = new AtomicBoolean( false );
	}

	/**
	 * Start the executor, allowing works to be submitted
	 * through {@link #submit(WorkSet)}.
	 *
	 * @param threadPoolProvider A provider of thread pools.
	 */
	public synchronized void start(ThreadPoolProvider threadPoolProvider) {
		executorService = threadPoolProvider.newFixedThreadPool( 1, name );
	}

	/**
	 * Stop the executor, no longer allowing works to be submitted
	 * through {@link #submit(WorkSet)}.
	 * <p>
	 * This will attempt to forcibly terminate currently executing works,
	 * and will remove pending works from the queue.
	 */
	public synchronized void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ExecutorService::shutdownNow, executorService );
			executorService = null;
			workQueue.clear();
			// It's possible that processing was successfully scheduled in the executor service but had no chance to run,
			// so we need to release waiting threads:
			if ( completionFuture != null ) {
				completionFuture.cancel( false );
				completionFuture = null;
			}
		}
	}

	/**
	 * Submit a set of works for execution.
	 * <p>
	 * Must not be called when the executor is stopped.
	 * @param workset A set of works to execute.
	 * @throws InterruptedException If the current thread is interrupted while enqueuing the workset.
	 */
	public void submit(W workset) throws InterruptedException {
		if ( executorService == null ) {
			throw new AssertionFailure(
					"Attempt to submit a workset to executor '" + name + "', which is stopped"
					+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
		workQueue.put( workset );
		ensureProcessingScheduled();
	}

	/**
	 * @return A future that completes when all works submitted to the executor so far are completely executed.
	 * Works submitted to the executor after entering this method may delay the wait.
	 */
	public CompletableFuture<?> getCompletion() {
		CompletableFuture<?> future = completionFuture;
		if ( future == null ) {
			// No processing in progress or scheduled.
			return CompletableFuture.completedFuture( null );
		}
		else {
			// Processing in progress or scheduled; the future will be completed when the queue becomes empty.
			return future;
		}
	}

	private void ensureProcessingScheduled() {
		if ( processingInProgress.compareAndSet( false, true ) ) {
			/*
			 * Our thread successfully flipped the boolean:
			 * processing wasn't in progress, and we're now responsible for scheduling it.
			 */
			try {
				if ( completionFuture == null ) {
					/*
					 * The executor was previously idle:
					 * we need to create a new future for the completion of the queue.
					 * This is not executed when re-scheduling processing between two batches.
					 */
					completionFuture = new CompletableFuture<>();
				}
				executorService.submit( this::processBatch );
			}
			catch (Throwable e) {
				/*
				 * Make sure a failure to submit the processing task
				 * to the executor service
				 * doesn't leave other threads waiting indefinitely.
				 */
				try {
					CompletableFuture<?> future = completionFuture;
					completionFuture = null;
					processingInProgress.set( false );
					future.completeExceptionally( e );
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
	}

	/**
	 * Takes a batch of worksets from the queue and processes them.
	 */
	private void processBatch() {
		try {
			CompletableFuture<?> batchFuture;
			synchronized (processor) {
				processor.beginBatch();
				workBuffer.clear();

				workQueue.drainTo( workBuffer, maxTasksPerBatch );

				for ( W workset : workBuffer ) {
					try {
						workset.submitTo( processor );
					}
					catch (Throwable e) {
						workset.markAsFailed( e );
					}
				}

				// Nothing more to do, end the batch and terminate
				batchFuture = processor.endBatch();
			}

			/*
			 * Wait for works to complete before trying to handle the next batch.
			 * Note: timeout is expected to be handled by the processor
			 * (the Elasticsearch client adds per-request timeouts, in particular),
			 * so this "join" will not last forever
			 */
			Futures.unwrappedExceptionJoin( batchFuture );
		}
		catch (Throwable e) {
			// This will only happen if there is a bug in the processor
			FailureContext.Builder contextBuilder = FailureContext.builder();
			contextBuilder.throwable( e );
			contextBuilder.failingOperation( "Work processing in executor '" + name + "'" );
			failureHandler.handle( contextBuilder.build() );
		}
		finally {
			// We're done executing this batch.
			if ( workQueue.isEmpty() ) {
				// We're done executing the whole queue: handle getCompletion().
				CompletableFuture<?> justFinishedQueueFuture = this.completionFuture;
				completionFuture = null;
				justFinishedQueueFuture.complete( null );
			}
			// Allow this thread (or others) to schedule processing again.
			processingInProgress.set( false );
			if ( !workQueue.isEmpty() ) {
				/*
				 * Either the work queue wasn't empty and the "if" block above wasn't executed,
				 * or the "if" block above was executed but someone submitted new work between
				 * the call to workQueue.isEmpty() and the call to processingInProgress.set( false ).
				 * In either case, we need to re-schedule processing, because no one else will.
				 */
				try {
					ensureProcessingScheduled();
				}
				catch (Throwable e) {
					// This will only happen if there is a bug in this class, but we don't want to fail silently
					FailureContext.Builder contextBuilder = FailureContext.builder();
					contextBuilder.throwable( e );
					contextBuilder.failingOperation( "Scheduling the next batch in executor '" + name + "'" );
					failureHandler.handle( contextBuilder.build() );
				}
			}
		}
	}

	public interface WorkProcessor {

		void beginBatch();

		/**
		 * Ensure all works submitted since the last call to {@link #beginBatch()} will actually be executed,
		 * along with any finishing task (commit, ...).
		 *
		 * @return A future completing when all works submitted since the last call to {@link #beginBatch()}
		 * have completed.
		 */
		CompletableFuture<?> endBatch();

	}

	public interface WorkSet<P extends WorkProcessor> {

		void submitTo(P processor);

		void markAsFailed(Throwable t);

	}

}
