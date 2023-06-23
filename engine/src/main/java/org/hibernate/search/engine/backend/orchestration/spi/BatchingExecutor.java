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
import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;
import org.hibernate.search.engine.logging.impl.Log;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An executor of works that accepts works from multiple threads, puts them in a queue,
 * and processes them in batches in a single background thread.
 * <p>
 * Useful when works can be merged together for optimization purposes (bulking in Elasticsearch),
 * or when they should never be executed in parallel (writes to a Lucene index).
 */
public final class BatchingExecutor<P extends BatchedWorkProcessor, W extends BatchedWork<? super P>> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );
	private static final BiConsumer<? super BatchedWork<?>, Throwable> ASYNC_FAILURE_REPORTER = BatchedWork::markAsFailed;

	private final String name;

	private final FailureHandler failureHandler;

	private final BlockingQueue<W> workQueue;
	private final BatchWorker<P, ? super W> worker;
	private final Consumer<? super W> blockingRetryProducer;

	private SingletonTask processingTask;

	/**
	 * @param name The name of the executor thread (and of this executor when reporting errors)
	 * @param processor A task processor. May not be thread-safe.
	 * @param maxTasksPerBatch The maximum number of tasks to process in a single batch.
	 * Higher values mean more opportunity for the processor to optimize execution, but higher heap consumption.
	 * @param fair if {@code true} tasks are always submitted to the
	 * processor in FIFO order, if {@code false} tasks submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 * @param blockingRetryProducer A retry work producer that would be called in case of offloading operation submitter and full queue.
	 */
	public BatchingExecutor(String name,
			P processor, int maxTasksPerBatch, boolean fair,
			FailureHandler failureHandler, Consumer<? super W> blockingRetryProducer) {
		this.name = name;
		this.failureHandler = failureHandler;
		this.blockingRetryProducer = blockingRetryProducer;
		this.workQueue = new ArrayBlockingQueue<>( maxTasksPerBatch, fair );
		this.worker = new BatchWorker<>( name, processor, workQueue, maxTasksPerBatch );
	}

	@Override
	public String toString() {
		return "BatchingExecutor["
				+ "name=" + name
				+ ", queue size=" + workQueue.size()
				+ ", processing=" + processingTask
				+ "]";
	}

	/**
	 * Start the executor, allowing works to be submitted
	 * through {@link #submit(BatchedWork, OperationSubmitter)}.
	 *
	 * @param executorService An executor service with at least one thread.
	 */
	public synchronized void start(SimpleScheduledExecutor executorService) {
		log.startingExecutor( name );
		processingTask = new SingletonTask(
				name, worker,
				new BatchScheduler( executorService ),
				failureHandler
		);
	}

	/**
	 * Stop the executor, no longer allowing works to be submitted
	 * through {@link #submit(BatchedWork, OperationSubmitter)}.
	 * <p>
	 * This will remove pending works from the queue.
	 */
	public synchronized void stop() {
		log.stoppingExecutor( name );

		workQueue.clear();

		// It's possible that processing was successfully scheduled in the executor service but had no chance to run,
		// so we need to release waiting threads:
		processingTask.stop();
		processingTask = null;
	}

	/**
	 * @deprecated Use {@link #submit(BatchedWork, OperationSubmitter)} instead.
	 */
	@Deprecated
	public void submit(W work) throws InterruptedException {
		submit( work, OperationSubmitter.blocking() );
	}

	/**
	 * Submit a work for execution.
	 * <p>
	 * Must not be called when the executor is stopped.
	 * @param work A work to execute.
	 * @param operationSubmitter How to handle request to submit operation when the queue is full.
	 * @throws InterruptedException If the current thread is interrupted while enqueuing the work.
	 */
	public void submit(W work, OperationSubmitter operationSubmitter) throws InterruptedException {
		if ( processingTask == null ) {
			throw new AssertionFailure(
					"Attempt to submit a work to executor '" + name + "', which is stopped."
			);
		}
		operationSubmitter.submitToQueue( workQueue, work, blockingRetryProducer, ASYNC_FAILURE_REPORTER );
		processingTask.ensureScheduled();
	}

	/**
	 * @return A future that completes when all works submitted to the executor so far are completely executed.
	 * Works submitted to the executor after entering this method may delay the wait.
	 */
	public CompletableFuture<?> completion() {
		if ( processingTask == null ) {
			// Not started
			return CompletableFuture.completedFuture( null );
		}
		return processingTask.completion();
	}

	/**
	 * Takes a batch of works from the queue and submits them to the processor.
	 */
	private static final class BatchWorker<P extends BatchedWorkProcessor, W extends BatchedWork<? super P>>
			implements SingletonTask.Worker {
		private final CompletableFuture<?> completedFuture = CompletableFuture.completedFuture( null );

		private final String name;
		private final P processor;
		private final BlockingQueue<W> workQueue;
		private final int maxTasksPerBatch;
		private final List<W> workBuffer;

		private BatchWorker(String name, P processor, BlockingQueue<W> workQueue,
				int maxTasksPerBatch) {
			this.name = name;
			this.processor = processor;
			this.workQueue = workQueue;
			this.maxTasksPerBatch = maxTasksPerBatch;
			this.workBuffer = new ArrayList<>( maxTasksPerBatch );
		}

		@Override
		public CompletableFuture<?> work() {
			workBuffer.clear();
			workQueue.drainTo( workBuffer, maxTasksPerBatch );

			if ( workBuffer.isEmpty() ) {
				// Nothing to do
				return completedFuture;
			}

			int workCount = workBuffer.size();
			boolean traceEnabled = log.isTraceEnabled();
			if ( traceEnabled ) {
				log.tracef( "Processing %d works in executor '%s'", workCount, name );
			}

			processor.beginBatch();
			for ( W work : workBuffer ) {
				try {
					work.submitTo( processor );
				}
				catch (Throwable e) {
					work.markAsFailed( e );
				}
			}

			// Nothing more to do, end the batch and terminate
			CompletableFuture<?> future = processor.endBatch();
			if ( traceEnabled ) {
				future.whenComplete( (result, throwable) -> {
					log.tracef( "Processed %d works in executor '%s'", workCount, name );
				} );
			}

			return future;
		}

		@Override
		public void complete() {
			processor.complete();
		}
	}

	private static final class BatchScheduler implements SingletonTask.Scheduler {
		private final SimpleScheduledExecutor delegate;

		public BatchScheduler(SimpleScheduledExecutor delegate) {
			this.delegate = delegate;
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			// Schedule the task for execution as soon as possible.
			return delegate.submit( runnable );
		}
	}

}
