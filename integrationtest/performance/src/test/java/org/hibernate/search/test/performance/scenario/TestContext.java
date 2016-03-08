/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import static org.hibernate.search.test.performance.util.Util.runGarbageCollectorAndWait;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.hibernate.SessionFactory;
import org.hibernate.search.test.performance.task.AbstractTask;
import org.hibernate.search.testsupport.TestConstants;

/**
 * @author Tomas Hradec
 */
public class TestContext {

	private static final Boolean PERFORMANCE_ENABLED = TestConstants.arePerformanceTestsEnabled();

	public static final boolean VERBOSE = PERFORMANCE_ENABLED;
	public static final boolean MEASURE_MEMORY = PERFORMANCE_ENABLED;
	public static final boolean MEASURE_TASK_TIME = PERFORMANCE_ENABLED;
	public static final boolean ASSERT_QUERY_RESULTS = true;
	public static final boolean CHECK_INDEX_STATE = true;
	public static final int MAX_AUTHORS = 1000;
	public static final int THREADS_COUNT = PERFORMANCE_ENABLED ? 20 : 2;

	public final SessionFactory sf;
	public final TestScenario scenario;
	public final ExecutorService executor = Executors.newFixedThreadPool( THREADS_COUNT );
	public final CountDownLatch startSignal = new CountDownLatch( 1 );

	public final List<AbstractTask> tasks = new CopyOnWriteArrayList<AbstractTask>();
	public final AtomicLong bookIdCounter = new AtomicLong( 0 );
	public final AtomicLong authorIdCounter = new AtomicLong( 0 );
	public final Random bookRandom = new Random();
	public final Random authorRandom = new Random();
	public final AtomicReference<RuntimeException> firstKnownError = new AtomicReference<>();

	public long startTime;
	public long stopTime;
	public long freeMemory;
	public long totalMemory;

	public TestContext(TestScenario testScenario, SessionFactory sf) {
		this.scenario = testScenario;
		this.sf = sf;

		if ( MEASURE_MEMORY ) {
			runGarbageCollectorAndWait();
			freeMemory = Runtime.getRuntime().freeMemory();
			totalMemory = Runtime.getRuntime().totalMemory();
		}
	}

	public long getRandomBookId() {
		long bookId = bookIdCounter.get();
		if ( bookId > 0 ) {
			return Math.abs( bookRandom.nextLong() ) % bookId;
		}
		return 0;
	}

	public long getRandomAutorId() {
		long authorId = authorIdCounter.get();
		if ( authorId > 0 ) {
			return Math.abs( authorRandom.nextLong() ) % authorIdCounter.get();
		}
		return 0;
	}

	public void startAndWait() {
		try {
			startTime = System.nanoTime();
			startSignal.countDown();
			executor.shutdown();
			executor.awaitTermination( 1, TimeUnit.DAYS );
			stopTime = System.nanoTime();
		}
		catch (InterruptedException e) {
			throw new RuntimeException( e );
		}
		RuntimeException firstRuntimeError = getFirstRuntimeError();
		if ( firstRuntimeError != null ) {
			throw firstRuntimeError;
		}
	}

	public void reportRuntimeException(RuntimeException e) {
		//We only want to track the first exception:
		//having any error is enough to invalidate the results,
		//and if there are multiple it's likely that they are either
		//repeated errors, or that others are caused by the first one.
		firstKnownError.compareAndSet( null, e );
	}

	private RuntimeException getFirstRuntimeError() {
		return firstKnownError.get();
	}

}
