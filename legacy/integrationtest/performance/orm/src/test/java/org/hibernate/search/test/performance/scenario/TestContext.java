/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import org.hibernate.SessionFactory;
import org.hibernate.search.testsupport.TestConstants;

import com.google.common.base.Stopwatch;

/**
 * @author Tomas Hradec
 */
public class TestContext {

	public final Boolean performanceEnabled = TestConstants.arePerformanceTestsEnabled();

	public final boolean verbose = performanceEnabled;
	public final boolean measureMemory = performanceEnabled;
	public final boolean measureTaskTime = performanceEnabled;
	public boolean initIndex = true;
	public final boolean assertQueryResults = true;
	public final boolean checkIndexState = true;

	public final SessionFactory sessionFactory;

	public long initialOffset;
	public long initialAuthorCount;
	public long initialBookCount;
	public long maxAuthors = 1000;

	public int threadCount;
	public long warmupCyclesCount;
	public long measuredCyclesCount;

	public final Stopwatch initDatabaseStopWatch = Stopwatch.createUnstarted();
	public final Stopwatch initIndexStopWatch = Stopwatch.createUnstarted();

	public TestContext(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		if ( performanceEnabled ) {
			this.initialOffset = 1_000_000;
			this.initialAuthorCount = 10_000;
			this.initialBookCount = 1_000_000;
		}
		else {
			this.initialOffset = 100;
			this.initialAuthorCount = 10;
			this.initialBookCount = 100;
		}

		/*
		 * Scenarios are to customize these values themselves
		 * based on their knowledge of the actual code being run.
		 * They should also adapt the values based on whether
		 * performance tests are enabled or not.
		 *
		 * We can't set sensible values here,
		 * because we don't know how expensive one cycle is,
		 * or whether parallel execution even makes sense.
		 */
		this.threadCount = 1;
		this.warmupCyclesCount = 1;
		this.measuredCyclesCount = 1;
	}

}
