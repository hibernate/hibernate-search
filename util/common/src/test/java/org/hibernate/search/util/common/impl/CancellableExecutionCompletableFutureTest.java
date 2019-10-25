/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.common.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.util.impl.test.FutureAssert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.awaitility.Awaitility;

public class CancellableExecutionCompletableFutureTest {

	private ExecutorService executorService;

	@Before
	public void setup() {
		executorService = new ThreadPoolExecutor(
				1, 1,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>( 10 )
		);
	}

	@After
	public void cleanup() {
		if ( executorService != null ) {
			executorService.shutdownNow();
		}
	}

	@Test
	public void runnable_success() {
		AtomicBoolean finished = new AtomicBoolean( false );
		CompletableFuture<Void> future = new CancellableExecutionCompletableFuture<>(
				() -> finished.set( true ),
				executorService
		);

		Awaitility.await().until( future::isDone );
		FutureAssert.assertThat( future ).isSuccessful();
		assertThat( finished ).isTrue();
	}

	@Test
	public void runnable_runtimeException() {
		AtomicBoolean finished = new AtomicBoolean( false );
		RuntimeException exception = new RuntimeException( "Some message" );
		CompletableFuture<Void> future = new CancellableExecutionCompletableFuture<>(
				() -> {
					try {
						throw exception;
					}
					finally {
						finished.set( true );
					}
				},
				executorService
		);

		Awaitility.await().until( future::isDone );
		FutureAssert.assertThat( future ).isFailed( exception );
		assertThat( finished ).isTrue();
	}

	@Test
	public void runnable_error() {
		AtomicBoolean finished = new AtomicBoolean( false );
		Error error = new Error( "Some message" );
		CompletableFuture<Void> future = new CancellableExecutionCompletableFuture<>(
				() -> {
					try {
						throw error;
					}
					finally {
						finished.set( true );
					}
				},
				executorService
		);

		Awaitility.await().until( future::isDone );
		FutureAssert.assertThat( future ).isFailed( error );
		assertThat( finished ).isTrue();
	}

	@Test
	public void runnable_cancel() throws InterruptedException {
		AtomicBoolean started = new AtomicBoolean( false );
		AtomicBoolean finished = new AtomicBoolean( false );
		CompletableFuture<Void> future = new CancellableExecutionCompletableFuture<>(
				() -> {
					started.set( true );
					try {
						// Wait forever
						Thread.sleep( Long.MAX_VALUE );
					}
					catch (InterruptedException e) {
						Thread.currentThread().interrupt();
						throw new RuntimeException( e );
					}
					finally {
						finished.set( true );
					}
				},
				executorService
		);

		Awaitility.await().untilTrue( started );
		// In some rare cases, we might need multiple cancellations:
		// after the runnable started, there is a small window of time during which it cannot be cancelled,
		// because its state within the executor hasn't been updated yet.
		Awaitility.await().untilAsserted(
				() -> assertThat( future.cancel( true ) ).isTrue()
		);

		Awaitility.await().until( future::isDone );
		FutureAssert.assertThat( future ).isCancelled();
		Awaitility.await().untilTrue( finished );
	}
}
