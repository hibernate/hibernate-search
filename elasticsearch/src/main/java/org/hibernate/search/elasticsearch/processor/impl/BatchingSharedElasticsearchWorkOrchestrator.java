/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.util.impl.Closer;
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
class BatchingSharedElasticsearchWorkOrchestrator implements ElasticsearchWorkOrchestrator, AutoCloseable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final FlushableElasticsearchWorkOrchestrator delegate;

	private final ScheduledExecutorService scheduler;
	private final BlockingDeque<Changeset> changesetQueue;
	private final AtomicBoolean processingScheduled;

	private volatile CountDownLatch lastAsyncWorkLatch;

	public BatchingSharedElasticsearchWorkOrchestrator(String name,
			FlushableElasticsearchWorkOrchestrator delegate) {
		this.delegate = delegate;
		changesetQueue = new LinkedBlockingDeque<>();
		scheduler = Executors.newScheduledThreadPool( name );
		processingScheduled = new AtomicBoolean( false );
	}

	@Override
	public CompletableFuture<Void> submit(Iterable<ElasticsearchWork<?>> works) {
		CompletableFuture<Void> future = new CompletableFuture<>();
		changesetQueue.add( new Changeset( works, future ) );
		ensureProcessingScheduled();
		return future;
	}

	private void ensureProcessingScheduled() {
		// Set up worker if needed
		if ( !processingScheduled.get() ) {
			synchronized ( this ) {
				if ( processingScheduled.compareAndSet( false, true ) ) {
					CountDownLatch latch = new CountDownLatch( 1 );
					try {
						scheduler.schedule( () -> processChangesets( latch ), 100, TimeUnit.MILLISECONDS );
						//only assign this when the job was successfully scheduled:
						lastAsyncWorkLatch = latch;
					}
					catch (Exception e) {
						// Make sure a failure to setup the worker doesn't leave other threads waiting indefinitely:
						processingScheduled.set( false );
						latch.countDown();
						throw e;
					}
				}
			}
		}
	}

	public void awaitCompletion() throws InterruptedException {
		final CountDownLatch localLatch = lastAsyncWorkLatch;
		if ( localLatch != null ) {
			localLatch.await();
		}
	}

	@Override
	public void close() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( () -> {
				scheduler.shutdown();
				try {
					scheduler.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
				}
				catch (InterruptedException e) {
					LOG.interruptedWhileWaitingForIndexActivity( e );
					Thread.currentThread().interrupt();
				}
			} );
			final CountDownLatch localLatch = lastAsyncWorkLatch;
			if ( localLatch != null ) {
				//It's possible that a task was successfully scheduled but had no chance to run,
				//so we need to release waiting threads:
				closer.push( localLatch::countDown );
			}
		}
	}

	/**
	 * Takes changesets from the queue and processes them.
	 */
	private void processChangesets(CountDownLatch latch) {
		try {
			synchronized ( this ) {
				try {
					delegate.reset();

					for ( Changeset changeset = changesetQueue.poll();
							changeset != null;
							changeset = changesetQueue.poll() ) {
						try {
							delegate.submit( changeset.works )
									.whenComplete( Futures.copyHandler( changeset.future ) );
						}
						catch (Throwable e) {
							changeset.future.completeExceptionally( e );
							throw e;
						}
					}
				}
				finally {
					// Allow other async processors to be setup
					processingScheduled.set( false );
				}

				// Nothing more to do, flush and terminate
				CompletableFuture<?> future = delegate.flush();

				// Note: timeout is handled by the client, so this "join" will not last forever
				future.join();
			}
		}
		finally {
			latch.countDown();
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
