package org.hibernate.search.backend.elasticsearch.client.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

class RestJdkClientTest {

	@Test
	void closeShutsDownExecutorServiceExplicitly() throws Exception {
		ExecutorService executor = mock( ExecutorService.class );
		HttpClient http = mock( HttpClient.class );
		when( http.executor() ).thenReturn( Optional.of( executor ) );
		RestJdkClient rest = new RestJdkClient( null, http );
		rest.close();
		verify( executor ).shutdownNow();
	}

	@Test
	void closeInterruptsExecutorTasksWithoutWaitingForTheirCompletion() throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		CountDownLatch started = new CountDownLatch( 1 );
		CountDownLatch interrupted = new CountDownLatch( 1 );
		CountDownLatch release = new CountDownLatch( 1 );
		HttpClient http = mock( HttpClient.class );
		when( http.executor() ).thenReturn( Optional.of( executor ) );
		RestJdkClient rest = new RestJdkClient( null, http );
		executor.submit( () -> {
			started.countDown();
			try {
				release.await();
			}
			catch (InterruptedException e) {
				interrupted.countDown();
				// Keep running until released to check that close does not wait indefinitely.
				try {
					release.await();
				}
				catch (InterruptedException secondInterrupt) {
					Thread.currentThread().interrupt();
				}
			}
		} );
		try {
			assertThat( started.await( 5, TimeUnit.SECONDS ) ).isTrue();
			CompletableFuture.runAsync( () -> {
				try {
					rest.close();
				}
				catch (Exception e) {
					throw new IllegalStateException( e );
				}
			} ).get( 5, TimeUnit.SECONDS );
			assertThat( executor.isShutdown() ).isTrue();
			assertThat( interrupted.await( 5, TimeUnit.SECONDS ) ).isTrue();
		}
		finally {
			release.countDown();
			executor.shutdownNow();
			assertThat( executor.awaitTermination( 5, TimeUnit.SECONDS ) ).isTrue();
		}
	}
}
