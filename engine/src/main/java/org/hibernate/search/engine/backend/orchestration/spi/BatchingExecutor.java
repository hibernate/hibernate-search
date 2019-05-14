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
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.AssertionFailure;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Executors;

/**
 * An executor of tasks that accepts tasks from multiple threads, puts them in a queue,
 * and processes them in batches in a single background thread.
 * <p>
 * Useful when tasks can be merged together for optimization purposes (bulking in Elasticsearch),
 * or when they should never be executed in parallel (writes to a Lucene index).
 */
public final class BatchingExecutor<T extends BatchingExecutor.Task<? super P>, P extends BatchingExecutor.Processor> {

	private final String name;

	private final P processor;
	private final ErrorHandler errorHandler;
	private final int maxTasksPerBatch;

	private final BlockingQueue<T> taskQueue;
	private final List<T> taskBuffer;
	private final AtomicBoolean processingScheduled;

	private ExecutorService executorService;

	private final Phaser phaser = new Phaser() {
		@Override
		protected boolean onAdvance(int phase, int registeredParties) {
			// This phaser never terminates on its own, allowing re-use
			return false;
		}
	};

	/**
	 * @param name The name of the executor thread (and of this executor when reporting errors)
	 * @param processor A task processor. May not be thread-safe.
	 * @param maxTasksPerBatch The maximum number of tasks to process in a single batch.
	 * Higher values mean more opportunity for the processor to optimize execution, but higher heap consumption.
	 * @param fair if {@code true} tasks are always submitted to the
	 * processor in FIFO order, if {@code false} tasks submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param errorHandler An error handler to report failures of the background thread.
	 */
	public BatchingExecutor(String name, P processor, int maxTasksPerBatch, boolean fair,
			ErrorHandler errorHandler) {
		this.name = name;
		this.processor = processor;
		this.errorHandler = errorHandler;
		this.maxTasksPerBatch = maxTasksPerBatch;
		taskQueue = new ArrayBlockingQueue<>( maxTasksPerBatch, fair );
		taskBuffer = new ArrayList<>( maxTasksPerBatch );
		processingScheduled = new AtomicBoolean( false );
	}

	/**
	 * Start the executor, allowing tasks to be submitted
	 * through {@link #submit(Task)}.
	 */
	public synchronized void start() {
		executorService = Executors.newFixedThreadPool( 1, name );
	}

	/**
	 * Stop the executor, no longer allowing tasks to be submitted
	 * through {@link #submit(Task)}.
	 * <p>
	 * This will attempt to forcibly terminate currently executing tasks,
	 * and will remove pending tasks from the queue.
	 */
	public synchronized void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ExecutorService::shutdownNow, executorService );
			executorService = null;
			taskQueue.clear();
			//It's possible that a task was successfully scheduled but had no chance to run,
			//so we need to release waiting threads:
			closer.push( Phaser::forceTermination, phaser );
		}
	}

	/**
	 * Submit a task for execution.
	 * <p>
	 * Must not be called when the executor is stopped.
	 * @param task A task to execute.
	 * @throws InterruptedException If the current thread is interrupted while enqueuing the task.
	 */
	public void submit(T task) throws InterruptedException {
		if ( executorService == null ) {
			throw new AssertionFailure(
					"Attempt to submit a task to executor '" + name + "', which is stopped"
					+ " There is probably a bug in Hibernate Search, please report it."
			);
		}
		taskQueue.put( task );
		ensureProcessingScheduled();
	}

	/**
	 * Block the current thread until the executor has completed all pending tasks.
	 * <p>
	 * Tasks submitted to the executor after entering this method
	 * may delay the wait.
	 *
	 * TODO HSEARCH-3576 we should use child executors, sharing the same execution service but each with its own child phaser,
	 *  allowing each child executor to only wait until all of its *own* tasks are completed.
	 *  That would however require to register/deregister the phaser for each single task,
	 *  instead of for each scheduling of the processing like we do now.
	 *  Phaser only support up to 65535 parties, so we would may need to use multiple phasers per child executor...
	 *
	 * @throws InterruptedException If the current thread is interrupted while waiting.
	 */
	public void awaitCompletion() throws InterruptedException {
		int phaseBeforeUnarrivedPartiesCheck = phaser.getPhase();
		if ( phaser.getUnarrivedParties() > 0 ) {
			phaser.awaitAdvanceInterruptibly( phaseBeforeUnarrivedPartiesCheck );
		}
	}

	private void ensureProcessingScheduled() {
		// Set up worker if needed
		if ( !processingScheduled.get() ) {
			/*
			 * Register to the phaser exactly here:
			 *  * registering after scheduling would mean running the risk
			 *  of finishing the work processing before we even registered to the phaser,
			 *  likely resulting in an exception when de-registering from the phaser;
			 *  * registering after compareAndSet would mean running the risk
			 *  of another thread calling this method just after we called compareAndSet,
			 *  then moving on to a call to awaitCompletion() before we had the chance to
			 *  register to the phaser. This other thread would thus believe that the submitted
			 *  work was executed while in fact it wasn't.
			 */
			phaser.register();
			try {
				if ( processingScheduled.compareAndSet( false, true ) ) {
					try {
						executorService.submit( this::processBatch );
					}
					catch (Throwable e) {
						/*
						 * Make sure a failure to submit the processing task
						 * doesn't leave other threads waiting indefinitely
						 */
						try {
							processingScheduled.set( false );
						}
						catch (Throwable e2) {
							e.addSuppressed( e2 );
						}
						throw e;
					}
				}
				else {
					/*
					 * Corner case: another thread submitted a processing task
					 * just after we registered the phaser.
					 * Cancel our own registration.
					 */
					phaser.arriveAndDeregister();
				}
			}
			catch (Throwable e) {
				/*
				 * Make sure a failure to submit the processing task
				 * doesn't leave other threads waiting indefinitely
				 */
				try {
					phaser.arriveAndDeregister();
				}
				catch (Throwable e2) {
					e.addSuppressed( e2 );
				}
				throw e;
			}
		}
	}

	/**
	 * Takes a batch of tasks from the queue and processes them.
	 */
	private void processBatch() {
		try {
			CompletableFuture<?> future;
			try {
				synchronized (processor) {
					processor.beginBatch();
					taskBuffer.clear();

					taskQueue.drainTo( taskBuffer, maxTasksPerBatch );

					for ( T task : taskBuffer ) {
						try {
							task.submitTo( processor );
						}
						catch (Throwable e) {
							task.markAsFailed( e );
							throw e;
						}
					}

					// Nothing more to do, end the batch and terminate
					future = processor.endBatch();
				}
			}
			finally {
				try {
					/*
					 * Allow processing to be scheduled immediately,
					 * even if we didn't finish executing yet (see the join below).
					 * This won't lead to concurrent processing,
					 * since there's only one thread in the pool,
					 * but it will make sure the processing delay runs from one
					 * queue drain to the next, instead of from one batch to
					 * the next, allowing higher throughput when batches take more
					 * than 100ms to process.
					 */
					processingScheduled.set( false );

					/*
					 * Just in case tasks were added to the queue between
					 * when we drained the queue and the resetting of
					 * processingScheduled above.
					 * This must be executed before we arrive at the phaser to ensure that
					 * threads calling submit(), then awaitCompletion() will not be unblocked
					 * before we called ensureProcessingScheduled() below.
					 */
					if ( !taskQueue.isEmpty() ) {
						ensureProcessingScheduled();
					}
				}
				catch (Throwable e) {
					// This will only happen if there is a bug in this class, but we don't want to fail silently
					errorHandler.handleException(
							"Error while ensuring the next task submitted to executor '" + name + "' will be processed",
							e
					);
				}
			}

			// Note: timeout is handled by the client, so this "join" will not last forever
			future.join();
		}
		catch (Throwable e) {
			// This will only happen if there is a bug in the processor
			errorHandler.handleException(
					"Error while processing tasks in executor '" + name + "'",
					e
			);
		}
		finally {
			/*
			 * Regardless of the outcome (exception or not),
			 * arrive at the phaser after all the works completed.
			 * Note that all works have a timeout, so this will be executed eventually.
			 *
			 * Also note this must be executed *after* the finally block above,
			 * so we are sure we won't arrive at the phaser before ensuring we're not
			 * in a situation where no processing is scheduled even though
			 * the queue is not empty.
			 */
			phaser.arriveAndDeregister();
		}
	}

	public interface Processor {

		void beginBatch();

		/**
		 * Ensure all tasks submitted since the last call to {@link #beginBatch()} will actually be executed,
		 * along with any finishing task (commit, ...).
		 *
		 * @return A future completing when all works submitted since the last call to {@link #beginBatch()}
		 * have completed.
		 */
		CompletableFuture<?> endBatch();

	}

	public interface Task<P extends Processor> {

		void submitTo(P processor);

		void markAsFailed(Throwable t);

	}

}
