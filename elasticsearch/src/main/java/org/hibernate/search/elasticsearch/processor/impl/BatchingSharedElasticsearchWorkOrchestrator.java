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
import java.util.concurrent.Phaser;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.logging.impl.LoggerFactory;


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
class BatchingSharedElasticsearchWorkOrchestrator implements BarrierElasticsearchWorkOrchestrator, AutoCloseable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final int delayMs;
	private final FlushableElasticsearchWorkOrchestrator delegate;
	private final ErrorHandler errorHandler;
	private final int changesetsPerBatch;

	private final ScheduledExecutorService scheduler;
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
	 * @param name The name of the orchestrator thread
	 * @param delayMs A delay before creating a batch when a work is submitted.
	 * Higher values mean bigger batch sizes, but higher latency.
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
			String name, int delayMs, int maxChangesetsPerBatch, boolean fair,
			FlushableElasticsearchWorkOrchestrator delegate,
			ErrorHandler errorHandler) {
		this.delayMs = delayMs;
		this.delegate = delegate;
		this.errorHandler = errorHandler;
		this.changesetsPerBatch = maxChangesetsPerBatch;
		changesetQueue = new ArrayBlockingQueue<>( maxChangesetsPerBatch, fair );
		changesetBuffer = CollectionHelper.newArrayList( maxChangesetsPerBatch );
		scheduler = Executors.newScheduledThreadPool( name );
		processingScheduled = new AtomicBoolean( false );
	}

	@Override
	public CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> works) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		try {
			changesetQueue.put( new Changeset( works, future ) );
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new SearchException( "Interrupted while submitting a changeset to the queue", e );
		}
		ensureProcessingScheduled();
		return future;
	}

	private void ensureProcessingScheduled() {
		// Set up worker if needed
		if ( !processingScheduled.get() ) {
			if ( processingScheduled.compareAndSet( false, true ) ) {
				try {
					scheduler.schedule( this::processBatch, delayMs, TimeUnit.MILLISECONDS );
					/*
					 * If scheduling succeeded, register to the phaser so that awaitCompletion()
					 * only returns after the processing succeeded
					 */
					phaser.register();
				}
				catch (Exception e) {
					// Make sure a failure to setup the worker doesn't leave other threads waiting indefinitely:
					processingScheduled.set( false );
					throw e;
				}
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
	public void close() {
		scheduler.shutdown();
		try {
			try {
				// Wait for all the pending tasks to be started
				scheduler.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
				// Wait for all the started tasks to be completed
				awaitCompletion();
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOG.interruptedWhileWaitingForIndexActivity( e );
			}
		}
		finally {
			//It's possible that a task was successfully scheduled but had no chance to run,
			//so we need to release waiting threads:
			phaser.forceTermination();
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

				// Note: timeout is handled by the client, so this "join" will not last forever
				future.join();
			}
			finally {
				try {
					// Allow processing to be scheduled again
					processingScheduled.set( false );

					/*
					 * Just in case changesets were added to the queue between
					 * when we drained the queue and the resetting of
					 * processingScheduled above.
					 * This must execute before we arrive at the phaser to ensure that
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

}
