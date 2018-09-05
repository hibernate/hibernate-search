/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Phaser;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.util.impl.Closer;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;


/**
 * An orchestrator sharing context across multiple threads,
 * allowing to batch together changesets from different threads,
 * and thus to produce bigger bulk works.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows to process more works when orchestrating, which allows to use bulk works
 * more extensively.
 *
 * @author Yoann Rodiere
 */
class BatchingSharedElasticsearchWorkOrchestrator extends AbstractBarrierElasticsearchWorkOrchestrator
		implements BarrierElasticsearchWorkOrchestrator, AutoCloseable {

	private static final Log LOG = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final FlushableElasticsearchWorkOrchestrator delegate;
	private final ErrorHandler errorHandler;
	private final int changesetsPerBatch;

	private final ExecutorService executor;
	private final BlockingQueue<Changeset> changesetQueue;
	private final List<Changeset> changesetBuffer;
	private final AtomicBoolean processingScheduled;

	private final Phaser phaser = new Phaser() {
		@Override
		protected boolean onAdvance(int phase, int registeredParties) {
			// This phaser never terminates on its own, allowing re-use
			return false;
		}
	};

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param maxChangesetsPerBatch The maximum number of changesets to
	 * process in a single batch. Higher values mean lesser chance of transport
	 * thread starvation, but higher heap consumption.
	 * @param fair if {@code true} changesets are always submitted to the
	 * delegate in FIFO order, if {@code false} changesets submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param delegate A delegate orchestrator. May not be thread-safe.
	 * @param errorHandler An error handler to send orchestration errors to.
	 */
	public BatchingSharedElasticsearchWorkOrchestrator(
			String name, int maxChangesetsPerBatch, boolean fair,
			FlushableElasticsearchWorkOrchestrator delegate,
			ErrorHandler errorHandler) {
		super( name );
		this.delegate = delegate;
		this.errorHandler = errorHandler;
		this.changesetsPerBatch = maxChangesetsPerBatch;
		changesetQueue = new ArrayBlockingQueue<>( maxChangesetsPerBatch, fair );
		changesetBuffer = CollectionHelper.newArrayList( maxChangesetsPerBatch );
		executor = Executors.newFixedThreadPool( 1, name );
		processingScheduled = new AtomicBoolean( false );
	}

	/**
	 * Create a child orchestrator.
	 * <p>
	 * The child orchestrator will use the same resources and its works
	 * will be executed by the same background thread.
	 * <p>
	 * Closing the child will not close the parent,
	 * but will make the current thread wait for the completion of previously submitted works,
	 * and will prevent any more work to be submitted through the child.
	 *
	 * @param name The name of the child orchestrator when reporting errors
	 */
	public BarrierElasticsearchWorkOrchestrator createChild(String name) {
		return new ChildOrchestrator( name );
	}

	@Override
	protected CompletableFuture<Void> doSubmit(Iterable<ElasticsearchWork<?>> works)
			throws InterruptedException {
		CompletableFuture<Void> future = new CompletableFuture<>();
		changesetQueue.put( new Changeset( works, future ) );
		ensureProcessingScheduled();
		return future;
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
						executor.submit( this::processBatch );
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

	@Override
	public void awaitCompletion() throws InterruptedException {
		int phaseBeforeUnarrivedPartiesCheck = phaser.getPhase();
		if ( phaser.getUnarrivedParties() > 0 ) {
			phaser.awaitAdvanceInterruptibly( phaseBeforeUnarrivedPartiesCheck );
		}
	}

	@Override
	protected void doClose() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( () -> awaitCompletionBeforeClose( getName() ) );
			closer.push( executor::shutdownNow );
			//It's possible that a task was successfully scheduled but had no chance to run,
			//so we need to release waiting threads:
			closer.push( phaser::forceTermination );
		}
	}

	private void awaitCompletionBeforeClose(String name) {
		try {
			awaitCompletion();
		}
		catch (InterruptedException e) {
			LOG.interruptedWhileWaitingForIndexActivity( name, e );
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Takes a batch of changesets from the queue and processes them.
	 */
	private void processBatch() {
		try {
			CompletableFuture<?> future;
			try {
				synchronized ( delegate ) {
					delegate.reset();
					changesetBuffer.clear();

					changesetQueue.drainTo( changesetBuffer, changesetsPerBatch );

					for ( Changeset changeset : changesetBuffer ) {
						try {
							delegate.submit( changeset.works )
									.whenComplete( Futures.copyHandler( changeset.future ) );
						}
						catch (Throwable e) {
							changeset.future.completeExceptionally( e );
							throw e;
						}
					}

					// Nothing more to do, flush and terminate
					future = delegate.flush();
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
					 * Just in case changesets were added to the queue between
					 * when we drained the queue and the resetting of
					 * processingScheduled above.
					 * This must be executed before we arrive at the phaser to ensure that
					 * threads calling submit(), then awaitCompletion() will not be unblocked
					 * before we called ensureProcessingScheduled() below.
					 */
					if ( !changesetQueue.isEmpty() ) {
						ensureProcessingScheduled();
					}
				}
				catch (Throwable e) {
					errorHandler.handleException(
							"Error while ensuring the next submitted asynchronous Elasticsearch works will be processed",
							e );
				}
			}

			// Note: timeout is handled by the client, so this "join" will not last forever
			future.join();
		}
		catch (Throwable e) {
			errorHandler.handleException( "Error while processing Elasticsearch works", e );
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

	private static class Changeset {
		private final Iterable<ElasticsearchWork<?>> works;
		private final CompletableFuture<Void> future;

		public Changeset(Iterable<ElasticsearchWork<?>> works, CompletableFuture<Void> future) {
			this.works = works;
			this.future = future;
		}
	}

	private class ChildOrchestrator extends AbstractBarrierElasticsearchWorkOrchestrator
			implements BarrierElasticsearchWorkOrchestrator {

		protected ChildOrchestrator(String name) {
			super( name );
		}

		@Override
		protected CompletableFuture<Void> doSubmit(Iterable<ElasticsearchWork<?>> works) throws InterruptedException {
			return BatchingSharedElasticsearchWorkOrchestrator.this.submit( works );
		}

		@Override
		public void awaitCompletion() throws InterruptedException {
			BatchingSharedElasticsearchWorkOrchestrator.this.awaitCompletion();
		}

		@Override
		protected void doClose() {
			BatchingSharedElasticsearchWorkOrchestrator.this.awaitCompletionBeforeClose( getName() );
		}
	}

}
