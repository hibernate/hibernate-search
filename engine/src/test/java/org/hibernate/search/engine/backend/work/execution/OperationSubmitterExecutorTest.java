/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.work.execution;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.engine.common.execution.spi.SimpleScheduledExecutor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class OperationSubmitterExecutorTest {

	private SimpleScheduledExecutor executor;
	@Before
	public void setUp() throws Exception {
		this.executor = new SimpleScheduledExecutor() {
			private BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>( 1 );
			private ThreadPoolExecutor delegate = new ThreadPoolExecutor( 1, 1,
					0L, TimeUnit.MILLISECONDS,
					queue
			);

			@Override
			public Future<?> submit(Runnable task) {
				while ( queue.size() == 1 ) {
					// should be ok in our controlled env to simulate a blocking submit.
				}
				return delegate.submit( task );
			}

			@Override
			public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
				throw new UnsupportedOperationException( "" );
			}

			@Override
			public void shutdownNow() {
				delegate.shutdownNow();
			}

			@Override
			public boolean isBlocking() {
				return true;
			}
		};
	}

	@After
	public void tearDown() throws Exception {
		this.executor.shutdownNow();
	}

	@Test
	public void blockingOperationSubmitterBlocksTheOperation() throws InterruptedException {
		BlockingTask blockingTask = new BlockingTask();
		executor.submit( blockingTask );
		executor.submit( blockingTask );

		CompletableFuture<Boolean> future = CompletableFuture.supplyAsync( () -> {
			try {
				OperationSubmitter.blocking().submitToExecutor( executor, () -> { }, r -> { }, (e, t) -> { } );
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			return true;
		} );

		// wait to give some time for the above future to actually block
		TimeUnit.SECONDS.sleep( 2 );

		//queue is full so future won't complete.
		assertThat( future.isDone() ).isFalse();

		//make some room in the queue:
		blockingTask.working.set( false );
		await().untilAsserted( () -> assertThat( future.isDone() ).isTrue() );
	}


	@Test
	public void nonBlockingOperationSubmitterThrowsException() {
		// rejecting submitter would just fail with exception all the time as our executor is blocking
		assertThatThrownBy( () -> OperationSubmitter.rejecting().submitToExecutor( executor, () -> { }, r -> { }, (e, t) -> { } ) )
				.isInstanceOf( RejectedExecutionException.class );
	}

	@Test
	public void nonBlockingOperationSubmitterWorksOk() throws InterruptedException {
		AtomicBoolean check = new AtomicBoolean( false );
		// if executor implements offer() rejecting can finish successfully:
		OperationSubmitter.rejecting().submitToExecutor(
				new SimpleScheduledExecutor() {
					@Override
					public Future<?> submit(Runnable task) {
						// doesn't matter as we implement offer()
						throw new UnsupportedOperationException();
					}

					@Override
					public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
						// doesn't matter as we implement offer()
						throw new UnsupportedOperationException();
					}

					@Override
					public void shutdownNow() {
						// doesn't matter as we implement offer()
						throw new UnsupportedOperationException();
					}

					@Override
					public boolean isBlocking() {
						// doesn't matter as we implement offer()
						throw new UnsupportedOperationException();
					}

					@Override
					public Future<?> offer(Runnable task) {
						return CompletableFuture.runAsync( task );
					}
				},
				() -> { check.set( true ); }, r -> { fail( "shouldn't happen." ); },
				(e, t) -> { }
		);

		await().untilAsserted( () -> assertThat( check ).isTrue() );
	}

	@Test
	public void offloadingSubmitterOffloads() throws Exception {
		BlockingTask blockingTask = new BlockingTask();
		executor.submit( blockingTask );

		// we won't submit to the queue but just make sure that work got offloaded
		AtomicBoolean worked = new AtomicBoolean( false );
		OperationSubmitter.offloading( Runnable::run ).submitToExecutor( executor, () -> { }, r -> { worked.set( true ); }, (e, t) -> { } );

		await().untilAsserted( () -> assertThat( worked ).isTrue() );
	}

	@Test
	public void offloadingSubmitterFailsToOffloadExceptionInProducer() throws Exception {
		BlockingTask blockingTask = new BlockingTask();
		executor.submit( blockingTask );

		AtomicBoolean worked = new AtomicBoolean( false );
		OperationSubmitter.offloading( Runnable::run ).submitToExecutor( executor, () -> { }, r -> { throw new IllegalStateException( "fail" ); },
				(e, t) -> {
					assertThat( t )
							.isInstanceOf( IllegalStateException.class )
							.hasMessageContaining( "fail" );
					worked.set( true );
				} );

		await().untilAsserted( () -> assertThat( worked ).isTrue() );
	}

	@Test
	public void offloadingSubmitterFailsToOffloadExceptionInAction() throws Exception {
		BlockingTask blockingTask = new BlockingTask();
		executor.submit( blockingTask );

		AtomicBoolean worked = new AtomicBoolean( false );
		OperationSubmitter.offloading( CompletableFuture::runAsync ).submitToExecutor( executor, () -> { throw new IllegalStateException( "fail" ); },
				Runnable::run,
				(e, t) -> {
					assertThat( t )
							.isInstanceOf( IllegalStateException.class )
							.hasMessageContaining( "fail" );
					worked.set( true );
				} );

		await().untilAsserted( () -> assertThat( worked ).isTrue() );
	}

	private static class BlockingTask implements Runnable {
		AtomicBoolean working = new AtomicBoolean( true );
		@Override
		public void run() {
			while ( working.get() ) {
				try {
					Thread.sleep( 1000 );
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}
