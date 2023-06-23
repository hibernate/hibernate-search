/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.concurrency;

import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Helper to create tests which need to execute multiple tasks at "approximately same time",
 * like stress tests.
 * Note that we assume the number of threads and tasks match.
 *
 * If any exception happens, a JUnit failure is caused.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 */
public class ConcurrentRunner {

	public static final int DEFAULT_REPEAT = 300;
	public static final int DEFAULT_THREADS = 30;

	private final ConcurrentMap<String, Throwable> failures = new ConcurrentHashMap<>( 0 );
	private final ExecutorService executor;
	private final CountDownLatch startLatch = new CountDownLatch( 1 );
	private final CountDownLatch mainTasksEndLatch;
	private final TaskFactory factory;
	private final int repetitions;
	private final CountDownLatch finalizingTaskEndLatch = new CountDownLatch( 1 );
	private Runnable finalizingTask = () -> {};

	private Long timeoutValue;
	private TimeUnit timeoutUnit;

	/**
	 * Provide a source for {@link Runnable} instances to run concurrently.
	 * This is meant to simplify collection and creation of such tasks.
	 * @param factory the source of Runnable instances to run
	 */
	public ConcurrentRunner(TaskFactory factory) {
		this( DEFAULT_REPEAT, DEFAULT_THREADS, factory );
	}

	/**
	 * /**
	 * Provide a source for {@link Runnable} instances to run concurrently.
	 * This is meant to simplify collection and creation of such tasks.
	 * @param repetitions the amount of times the task should be repeated
	 * @param threads the number of threads used to run the task in parallel
	 * @param factory the source of Runnable instances to run.
	 */
	public ConcurrentRunner(int repetitions, int threads, TaskFactory factory) {
		this.repetitions = repetitions;
		this.factory = factory;
		executor = Executors.newFixedThreadPool( threads );
		mainTasksEndLatch = new CountDownLatch( repetitions );
	}

	/**
	 * Add a runnable to be executed regardless of the tasks produced by the factory.
	 * <p>
	 * Used to wait for asynchronous tasks to finish, for instance.
	 * <p>
	 * Execution time will be accounted for when assessing timeouts.
	 *
	 * @param finalizingTask The runnable to execute
	 * @return The runner, for chained calls.
	 */
	public ConcurrentRunner setFinalizingTask(Runnable finalizingTask) {
		this.finalizingTask = finalizingTask;
		return this;
	}

	public ConcurrentRunner setTimeout(long timeoutValue, TimeUnit timeoutUnit) {
		this.timeoutValue = timeoutValue;
		this.timeoutUnit = timeoutUnit;
		return this;
	}

	/**
	 * Invokes the {@link TaskFactory} and runs all the built tasks in
	 * an Executor.
	 * @throws Exception if any exception is thrown during the creation of tasks.
	 * @throws AssertionError if interrupted or any exception is thrown by the tasks.
	 */
	public void execute() throws Exception, AssertionError {
		for ( int i = 0; i < repetitions; i++ ) {
			Runnable userRunnable = factory.createRunnable( i );
			executor.execute( new WrapRunnable( startLatch, mainTasksEndLatch, "#" + i, userRunnable ) );
		}
		// When all other tasks finished executing, execute the finalizing task
		executor.execute( new WrapRunnable(
				mainTasksEndLatch, finalizingTaskEndLatch, "'finalizing task'", finalizingTask
		) );
		executor.shutdown();
		startLatch.countDown();

		boolean timedOut = false;
		try {
			if ( timeoutValue != null ) {
				if ( !finalizingTaskEndLatch.await( timeoutValue, timeoutUnit ) ) {
					executor.shutdownNow();
					timedOut = true;
				}
			}
			else {
				finalizingTaskEndLatch.await();
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
			fail( "Interrupted while waiting for end of execution" );
		}

		AssertionError reportedError = null;

		if ( timedOut ) {
			reportedError =
					new AssertionError( "The thread pool didn't finish executing after " + timeoutValue + " " + timeoutUnit );
			// Go on and also add errors (if any) as suppressed exceptions
		}

		for ( Map.Entry<String, Throwable> entry : failures.entrySet() ) {
			if ( reportedError == null ) {
				reportedError = new AssertionError( "Unexpected failure on task " + entry.getKey(), entry.getValue() );
			}
			else {
				reportedError.addSuppressed( entry.getValue() );
			}
		}

		if ( reportedError != null ) {
			throw reportedError;
		}
	}

	private class WrapRunnable implements Runnable {

		private final CountDownLatch startLatch;
		private final CountDownLatch endLatch;
		private final String taskName;
		private final Runnable userRunnable;

		public WrapRunnable(CountDownLatch startLatch, CountDownLatch endLatch, String taskName, Runnable userRunnable) {
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.taskName = taskName;
			this.userRunnable = userRunnable;
		}

		@Override
		public void run() {
			try {
				startLatch.await(); // Maximize chances of working concurrently on the Serializer
				//Prevent more work to be scheduled if something failed already
				if ( failures.isEmpty() ) {
					userRunnable.run();
				}
			}
			catch (InterruptedException | RuntimeException | AssertionError e) {
				failures.put( taskName, e );
			}
			endLatch.countDown();
		}

	}

	public interface TaskFactory {
		Runnable createRunnable(int i) throws Exception;
	}

}
