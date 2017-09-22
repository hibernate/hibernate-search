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

	private static final Boolean PERFORMANCE_ENABLED = TestConstants.arePerformanceTestsEnabled();

	public static final boolean VERBOSE = PERFORMANCE_ENABLED;
	public static final boolean MEASURE_MEMORY = PERFORMANCE_ENABLED;
	public static final boolean MEASURE_TASK_TIME = PERFORMANCE_ENABLED;
	public static final boolean ASSERT_QUERY_RESULTS = true;
	public static final boolean CHECK_INDEX_STATE = true;
	public static final int MAX_AUTHORS = 1000;

	public static final int THREADS_COUNT = PERFORMANCE_ENABLED ? 20 : 2;

	public final SessionFactory sessionFactory;

	public long initialOffset;
	public long initialAuthorCount;
	public long initialBookCount;
	public long warmupCyclesCount;
	public long measuredCyclesCount;

	public final Stopwatch initDatabaseStopWatch = Stopwatch.createUnstarted();
	public final Stopwatch initIndexStopWatch = Stopwatch.createUnstarted();

	public TestContext(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;

		if ( PERFORMANCE_ENABLED ) {
			this.initialOffset = 1_000_000;
			this.initialAuthorCount = 10_000;
			this.initialBookCount = 1_000_000;
			this.warmupCyclesCount = 1_000;
			this.measuredCyclesCount = 5_000;
		}
		else {
			this.initialOffset = 100;
			this.initialAuthorCount = 10;
			this.initialBookCount = 100;
			this.warmupCyclesCount = 1;
			this.measuredCyclesCount = 1;
		}
	}

}
