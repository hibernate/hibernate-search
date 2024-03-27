/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.hibernate.search.engine.reporting.EntityIndexingFailureContext;
import org.hibernate.search.engine.reporting.FailureContext;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.impl.test.annotation.TestForIssue;

import org.junit.jupiter.api.Test;

import org.awaitility.Awaitility;

@TestForIssue(jiraKey = "HSEARCH-4363")
class SingletonTaskTest {

	private final TestWorker worker = new TestWorker();
	private final TestFailureHandler failureHandler = new TestFailureHandler();

	private final SingletonTask testSubject = new SingletonTask(
			"TestSingletonTask", worker, new TestScheduler(), failureHandler
	);

	@Test
	void stopWhileBlockingWorkInProgress() throws Exception {
		testSubject.ensureScheduled();

		Awaitility.await().until( () -> worker.started );

		testSubject.stop();

		worker.future.complete( null );

		// we don't expect any failures
		assertThat( failureHandler.failures ).isEmpty();
	}

	private static final class TestWorker implements SingletonTask.Worker {

		private final CompletableFuture<Void> future = new CompletableFuture<>();
		private volatile boolean started = false;

		@Override
		public CompletableFuture<?> work() {
			started = true;
			future.join();
			return future;
		}

		@Override
		public void complete() {
			// Nothing to do.
		}
	}

	private static final class TestScheduler implements SingletonTask.Scheduler {

		private final ExecutorService delegate = Executors.newSingleThreadExecutor();

		public TestScheduler() {
		}

		@Override
		public Future<?> schedule(Runnable runnable) {
			// Schedule the task for execution as soon as possible.
			return delegate.submit( runnable );
		}
	}

	private static final class TestFailureHandler implements FailureHandler {

		private final List<FailureContext> failures = new ArrayList<>();

		@Override
		public void handle(FailureContext context) {
			failures.add( context );
		}

		@Override
		public void handle(EntityIndexingFailureContext context) {
			// Nothing to do.
		}
	}
}
