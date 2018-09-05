/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import org.hibernate.search.test.performance.task.InsertBookTask;
import org.hibernate.search.test.performance.task.QueryBooksByAuthorTask;
import org.hibernate.search.test.performance.task.QueryBooksByAverageRatingTask;
import org.hibernate.search.test.performance.task.QueryBooksByBestRatingTask;
import org.hibernate.search.test.performance.task.QueryBooksByNewestPublishedTask;
import org.hibernate.search.test.performance.task.QueryBooksBySummaryTask;
import org.hibernate.search.test.performance.task.QueryBooksByTitleTask;
import org.hibernate.search.test.performance.task.QueryBooksByTotalSoldTask;
import org.hibernate.search.test.performance.task.UpdateBookRatingTask;
import org.hibernate.search.test.performance.task.UpdateBookTotalSoldTask;

/**
 * @author Tomas Hradec
 */
public abstract class ReadWriteTestScenario extends AbstractTestScenario {

	@Override
	protected void initContext(TestContext testContext) {
		super.initContext( testContext );
		if ( testContext.performanceEnabled ) {
			testContext.warmupCyclesCount = 1_000;
			testContext.measuredCyclesCount = 5_000;
			testContext.threadCount = 20;
		}
		else {
			testContext.warmupCyclesCount = 1;
			testContext.measuredCyclesCount = 1;
			testContext.threadCount = 2;
		}
	}

	@Override
	protected void addTasks(TestScenarioContext ctx) {
		// Note: each task will register itself to the context
		new InsertBookTask( ctx );
		new UpdateBookRatingTask( ctx );
		new UpdateBookTotalSoldTask( ctx );
		new QueryBooksByAuthorTask( ctx );
		new QueryBooksByAverageRatingTask( ctx );
		new QueryBooksByBestRatingTask( ctx );
		new QueryBooksByNewestPublishedTask( ctx );
		new QueryBooksBySummaryTask( ctx );
		new QueryBooksByTitleTask( ctx );
		new QueryBooksByTotalSoldTask( ctx );
	}

}
