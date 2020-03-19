/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An executor of works that accepts works from multiple threads, puts them in a queue,
 * and processes them in batches in a single background thread.
 * <p>
 * Useful when works can be merged together for optimization purposes (bulking in Elasticsearch),
 * or when they should never be executed in parallel (writes to a Lucene index).
 */
public final class BatchingExecutor<W extends BatchingExecutor.WorkSet<? super P>, P extends BatchingExecutor.WorkProcessor> {

	private final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final P processor;
	private final FailureHandler failureHandler;
	private final int maxTasksPerBatch;

	private final BlockingQueue<W> workQueue;
	private final List<W> workBuffer;
	private final AtomicReference<ProcessingStatus> processingStatus;

	private ExecutorService executorService;
	private ScheduledExecutorService scheduledExecutorService;
	private volatile CompletableFuture<?> completionFuture;
	private volatile ScheduledFuture<?> scheduledNextProcessing;

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
		processingStatus = new AtomicReference<>( ProcessingStatus.IDLE );
	}

	/**
	 * Start the executor, allowing works to be submitted
	 * through {@link #submit(WorkSet)}.
	 *
	 * @param threadPoolProvider A provider of thread pools.
	 */
	public synchronized void start(ThreadPoolProvider threadPoolProvider) {
		log.startingExecutor( name );
		executorService = threadPoolProvider.newFixedThreadPool( 1, name );
		scheduledExecutorService = threadPoolProvider.getSharedScheduledThreadPool();
	}

	/**
	 * Stop the executor, no longer allowing works to be submitted
	 * through {@link #submit(WorkSet)}.
	 * <p>
	 * This will attempt to forcibly terminate currently executing works,
	 * and will remove pending works from the queue.
	 */
	public synchronized void stop() {
		log.stoppingExecutor( name );
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			// scheduledExecutorService is not ours to close: it's shared
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
		ensureProcessingRunning();
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

	private void ensureProcessingRunning() {
		if ( !processingStatus.compareAndSet( ProcessingStatus.IDLE, ProcessingStatus.RUNNING ) ) {
			// Already running
			return;
		}

		/*
		 * Our thread successfully switched the status:
		 * processing wasn't in progress, and we're now responsible for scheduling it.
		 */
		try {
			if ( scheduledNextProcessing != null ) {
				/*
				 * We scheduled processing at a later time.
				 * Since we're going to execute processing right now,
				 * we can cancel this scheduling.
				 */
				scheduledNextProcessing.cancel( false );
				scheduledNextProcessing = null;
			}
			if ( completionFuture == null ) {
				/*
				 * The executor was previously idle:
				 * we need to create a new future for the completion of the queue.
				 * This is not executed when re-scheduling processing between two batches.
				 */
				completionFuture = new CompletableFuture<>();
			}
			executorService.submit( this::process );
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
				processingStatus.set( ProcessingStatus.IDLE );
				future.completeExceptionally( e );
			}
			catch (Throwable e2) {
				e.addSuppressed( e2 );
			}
			throw e;
		}
	}

	/**
	 * Takes a batch of worksets from the queue and processes them.
	 */
	private void process() {
		try {
			workBuffer.clear();
			workQueue.drainTo( workBuffer, maxTasksPerBatch );

			if ( !workBuffer.isEmpty() ) {
				processBatch( workBuffer );
			}
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
			try {
				if ( workQueue.isEmpty() ) {
					// We managed to process the whole queue.
					// Inform the processor and callers.
					handleCompletion();
				}
				// Allow this thread (or others) to run processing again.
				processingStatus.set( ProcessingStatus.IDLE );
				// Call workQueue.isEmpty() again, since its content may have changed since the last call a few lines above.
				if ( !workQueue.isEmpty() ) {
					// There are still worksets in the queue.
					// Make sure they will be processed.
					ensureProcessingRunning();
				}
			}
			catch (Throwable e) {
				// This will only happen if there is a bug in this class, but we don't want to fail silently
				FailureContext.Builder contextBuilder = FailureContext.builder();
				contextBuilder.throwable( e );
				contextBuilder.failingOperation( "Handling post-execution in executor '" + name + "'" );
				failureHandler.handle( contextBuilder.build() );
			}
		}
	}

	private void processBatch(List<W> works) {
		processor.beginBatch();

		for ( W workset : works ) {
			try {
				workset.submitTo( processor );
			}
			catch (Throwable e) {
				workset.markAsFailed( e );
			}
		}

		// Nothing more to do, end the batch and terminate
		CompletableFuture<?> batchFuture = processor.endBatch();

		/*
		 * Wait for works to complete before trying to handle the next batch.
		 * Note: timeout is expected to be handled by the processor
		 * (the Elasticsearch client adds per-request timeouts, in particular),
		 * so this "join" will not last forever
		 */
		Futures.unwrappedExceptionJoin( batchFuture );
	}

	private void handleCompletion() {
		// First, tell the processor that we're done processing.
		long delay = 0;
		try {
			delay = processor.completeOrDelay();
		}
		catch (Throwable e) {
			// This will only happen if there is a bug in this class, but we don't want to fail silently
			FailureContext.Builder contextBuilder = FailureContext.builder();
			contextBuilder.throwable( e );
			contextBuilder.failingOperation( "Calling processor.complete() in executor '" + name + "'" );
			failureHandler.handle( contextBuilder.build() );
		}

		if ( delay <= 0 ) {
			// The processor acknowledged that all outstanding operations (commit, ...) have completed.
			// Tell callers of getCompletion()
			CompletableFuture<?> justFinishedQueueFuture = this.completionFuture;
			completionFuture = null;
			justFinishedQueueFuture.complete( null );
		}
		else {
			// The processor requested that we wait because some outstanding operations (commit, ...)
			// need to be performed later.
			scheduledNextProcessing = scheduledExecutorService.schedule(
					this::ensureProcessingRunning, delay, TimeUnit.MILLISECONDS
			);
		}
	}

	public interface WorkProcessor {

		/**
		 * Initializes internal state before works are submitted.
		 */
		void beginBatch();

		/**
		 * Ensures all works submitted
		 * since the last call to {@link #beginBatch()}
		 * will actually be executed, along with any finishing task (commit, ...).
		 *
		 * @return A future completing when the executor is allowed to start another batch.
		 */
		CompletableFuture<?> endBatch();

		/**
		 * Executes any outstanding operation if possible, or return an estimation of when they can be executed.
		 * <p>
		 * Called when the executor considers the work queue complete
		 * and does not plan on submitting another batch due to work starvation.
		 *
		 * @return {@code 0} if there is no outstanding operation, or a positive number of milliseconds
		 * if there are outstanding operations and {@link #completeOrDelay()}
		 * must be called again that many milliseconds later.
		 */
		long completeOrDelay();

	}

	public interface WorkSet<P extends WorkProcessor> {

		void submitTo(P processor);

		void markAsFailed(Throwable t);

	}

	public enum ProcessingStatus {

		IDLE,
		RUNNING

	}

}
