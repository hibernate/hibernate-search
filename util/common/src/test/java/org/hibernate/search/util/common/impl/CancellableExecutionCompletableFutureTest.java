/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.util.common.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.search.util.impl.test.FutureAssert.assertThatFuture;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.awaitility.Awaitility;

class CancellableExecutionCompletableFutureTest {

	private ExecutorService executorService;

	@BeforeEach
	void setup() {
		executorService = new ThreadPoolExecutor(
				1, 1,
				0L,
				TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<>( 10 )
		);
	}

	@AfterEach
	void cleanup() {
		if ( executorService != null ) {
			executorService.shutdownNow();
		}
	}

	@Test
	void runnable_success() {
		AtomicBoolean finished = new AtomicBoolean( false );
		CompletableFuture<Void> future = new CancellableExecutionCompletableFuture<>(
				() -> finished.set( true ),
				executorService
		);

		Awaitility.await().until( future::isDone );
		assertThatFuture( future ).isSuccessful();
		assertThat( finished ).isTrue();
	}

	@Test
	void runnable_runtimeException() {
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
		assertThatFuture( future ).isFailed( exception );
		assertThat( finished ).isTrue();
	}

	@Test
	void runnable_error() {
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
		assertThatFuture( future ).isFailed( error );
		assertThat( finished ).isTrue();
	}

	@Test
	void runnable_cancel() throws InterruptedException {
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
		assertThatFuture( future ).isCancelled();
		Awaitility.await().untilTrue( finished );

		// Also test that the failure triggered by the cancellation ultimately
		// gets reported as a suppressed exception.
		Awaitility.await().untilAsserted( () -> assertThat( Futures.getThrowableNow( future ) )
				.extracting( Throwable::getSuppressed ).asInstanceOf( InstanceOfAssertFactories.ARRAY )
				.hasSize( 1 )
				.extracting( Function.identity() ) // Hack to get access to the ".first()" method
				.first()
				.asInstanceOf( InstanceOfAssertFactories.THROWABLE )
				.isInstanceOf( RuntimeException.class )
				.hasCauseInstanceOf( InterruptedException.class )
		);
	}
}
