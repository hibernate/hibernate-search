/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.testsupport.concurrency;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;


/**
 * Helper to create tests which need to execute multiple tasks at "approximately same time",
 * like stress tests.
 * Note that we assume the number of threads and tasks match.
 *
 * If any exception happens, a JUnit failure is caused.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2014 Red Hat Inc.
 */
public class ConcurrentRunner {

	public static final int DEFAULT_REPEAT = 300;
	public static final int DEFAULT_THREADS = 30;

	private final AtomicBoolean somethingFailed = new AtomicBoolean( false );
	private final ExecutorService executor;
	private final CountDownLatch startLatch = new CountDownLatch( 1 );
	private final CountDownLatch endLatch;
	private final TaskFactory factory;
	private final int repetitions;

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
		endLatch = new CountDownLatch( repetitions );
	}

	/**
	 * Invokes the {@link TaskFactory} and runs all the built tasks in
	 * an Executor.
	 * @throws Exception if interrupted, any exception is thrown by the tasks,
	 * or any exception is thrown during the creation of tasks.
	 */
	public void execute() throws Exception {
		for ( int i = 0; i < repetitions; i++ ) {
			Runnable userRunnable = factory.createRunnable( i );
			executor.execute( new WrapRunnable( startLatch, endLatch, userRunnable ) );
		}
		executor.shutdown();
		startLatch.countDown();
		try {
			endLatch.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			e.printStackTrace();
			Assert.fail( "Interrupted while awaiting for end of execution" );
		}
		Assert.assertFalse( somethingFailed.get() );
	}

	private class WrapRunnable implements Runnable {

		private final CountDownLatch startLatch;
		private final CountDownLatch endLatch;
		private final Runnable userRunnable;

		public WrapRunnable(CountDownLatch startLatch, CountDownLatch endLatch, Runnable userRunnable) {
			this.startLatch = startLatch;
			this.endLatch = endLatch;
			this.userRunnable = userRunnable;
		}

		@Override
		public void run() {
			try {
				startLatch.await(); // Maximize chances of working concurrently on the Serializer
				//Prevent more work to be scheduled if something failed already
				if ( somethingFailed.get() == false ) {
					userRunnable.run();
				}
			}
			catch (InterruptedException | RuntimeException | AssertionError e) {
				e.printStackTrace();
				somethingFailed.set( true );
			}
			endLatch.countDown();
		}

	}

	public interface TaskFactory {
		Runnable createRunnable(int i) throws Exception;
	}

}
