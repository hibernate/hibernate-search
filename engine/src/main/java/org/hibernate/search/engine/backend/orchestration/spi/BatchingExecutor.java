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
import java.util.concurrent.Future;

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
public final class BatchingExecutor<P extends BatchedWorkProcessor> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final String name;

	private final FailureHandler failureHandler;

	private final BlockingQueue<BatchedWork<? super P>> workQueue;
	private final BatchWorker<P> worker;

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
	 */
	public BatchingExecutor(String name,
			P processor, int maxTasksPerBatch, boolean fair,
			FailureHandler failureHandler) {
		this.name = name;
		this.failureHandler = failureHandler;
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
	 * through {@link #submit(BatchedWork)}.
	 *
	 * @param executorService An executor service with at least one thread.
	 */
	public synchronized void start(ExecutorService executorService) {
		log.startingExecutor( name );
		processingTask = new SingletonTask(
				name, worker,
				new BatchScheduler( executorService ),
				failureHandler
		);
	}

	/**
	 * Stop the executor, no longer allowing works to be submitted
	 * through {@link #submit(BatchedWork)}.
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
	 * Submit a work for execution.
	 * <p>
	 * Must not be called when the executor is stopped.
	 * @param work A work to execute.
	 * @throws InterruptedException If the current thread is interrupted while enqueuing the work.
	 */
	public void submit(BatchedWork<? super P> work) throws InterruptedException {
		if ( processingTask == null ) {
			throw new AssertionFailure(
					"Attempt to submit a work to executor '" + name + "', which is stopped."
			);
		}
		workQueue.put( work );
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
	private static final class BatchWorker<P extends BatchedWorkProcessor> implements SingletonTask.Worker {
		private final CompletableFuture<?> completedFuture = CompletableFuture.completedFuture( null );

		private final String name;
		private final P processor;
		private final BlockingQueue<BatchedWork<? super P>> workQueue;
		private final int maxTasksPerBatch;
		private final List<BatchedWork<? super P>> workBuffer;

		private BatchWorker(String name, P processor, BlockingQueue<BatchedWork<? super P>> workQueue,
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
			boolean debugEnabled = log.isDebugEnabled();
			if ( debugEnabled ) {
				log.debugf( "Processing %d works in executor '%s'", workCount, name );
			}

			processor.beginBatch();

			for ( BatchedWork<? super P> work : workBuffer ) {
				try {
					work.submitTo( processor );
				}
				catch (Throwable e) {
					work.markAsFailed( e );
				}
			}

			// Nothing more to do, end the batch and terminate
			CompletableFuture<?> future = processor.endBatch();
			if ( debugEnabled ) {
				future.whenComplete( (result, throwable) -> {
					log.debugf( "Processed %d works in executor '%s'", workCount, name );
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
		private final ExecutorService delegate;

		public BatchScheduler(ExecutorService delegate) {
			this.delegate = delegate;
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			// Schedule the task for execution as soon as possible.
			return delegate.submit( runnable );
		}
	}

}
