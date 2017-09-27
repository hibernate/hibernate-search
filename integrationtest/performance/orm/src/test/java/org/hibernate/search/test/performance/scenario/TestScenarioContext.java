/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.hibernate.search.test.performance.task.AbstractTask;

import com.google.common.base.Stopwatch;

import static org.hibernate.search.test.performance.util.Util.runGarbageCollectorAndWait;

/**
 * @author Tomas Hradec
 */
public class TestScenarioContext {

	public final TestContext testContext;
	public final TestScenario scenario;

	public final CountDownLatch startSignal = new CountDownLatch( 1 );
	public final List<AbstractTask> tasks = new CopyOnWriteArrayList<AbstractTask>();

	public final AtomicLong bookIdCounter = new AtomicLong( 0 );
	public final AtomicLong authorIdCounter = new AtomicLong( 0 );
	public final Random bookRandom = new Random();
	public final Random authorRandom = new Random();
	private final Collection<Throwable> failures = new ConcurrentLinkedDeque<>();

	public final Stopwatch executionStopWatch = Stopwatch.createUnstarted();
	public final long initialFreeMemory;
	public final long initialTotalMemory;

	public TestScenarioContext(TestContext testContext, TestScenario testScenario) {
		this.testContext = testContext;
		this.scenario = testScenario;

		if ( testContext.measureMemory ) {
			runGarbageCollectorAndWait();
			initialFreeMemory = Runtime.getRuntime().freeMemory();
			initialTotalMemory = Runtime.getRuntime().totalMemory();
		}
		else {
			initialFreeMemory = -1;
			initialTotalMemory = -1;
		}
	}

	public long getRandomBookId() {
		long bookId = bookIdCounter.get();
		if ( bookId > 0 ) {
			return Math.abs( bookRandom.nextLong() ) % bookId;
		}
		return 0;
	}

	public long getRandomAuthorId() {
		long authorId = authorIdCounter.get();
		if ( authorId > 0 ) {
			return Math.abs( authorRandom.nextLong() ) % authorIdCounter.get();
		}
		return 0;
	}

	public void reportFailure(Throwable t) {
		failures.add( t );
	}

	public Collection<Throwable> getFailures() {
		return Collections.unmodifiableCollection( failures );
	}

}
