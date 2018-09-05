/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.scenario;

import java.util.Properties;

import org.hibernate.search.test.performance.task.Jsr352MassIndexerTask;
import org.hibernate.search.test.performance.task.QueryBooksByAuthorTask;
import org.hibernate.search.test.performance.task.QueryBooksByAverageRatingTask;
import org.hibernate.search.test.performance.task.QueryBooksByBestRatingTask;
import org.hibernate.search.test.performance.task.QueryBooksByNewestPublishedTask;
import org.hibernate.search.test.performance.task.QueryBooksBySummaryTask;
import org.hibernate.search.test.performance.task.QueryBooksByTitleTask;
import org.hibernate.search.test.performance.task.QueryBooksByTotalSoldTask;

/**
 * @author Yoann Rodiere
 */
public abstract class Jsr352MassIndexerTestScenario extends AbstractTestScenario {

	@Override
	public Properties getHibernateProperties() {
		return super.getHibernateProperties();
	}

	@Override
	protected void initContext(TestContext testContext) {
		super.initContext( testContext );
		/*
		 * We don't want the index to be initialized by the executor,
		 * since index initialization is what we are testing
		 */
		testContext.initIndex = false;

		// Multi-threading does not make sense here
		testContext.threadCount = 1;

		if ( testContext.performanceEnabled ) {
			testContext.warmupCyclesCount = 1;
			testContext.measuredCyclesCount = 5;
		}
	}

	@Override
	protected void addTasks(TestScenarioContext ctx) {
		// Note: each task will register itself to the context
		new Jsr352MassIndexerTask( ctx );
		// Add query tasks, just to check that indexing worked correctly
		new QueryBooksByAuthorTask( ctx );
		new QueryBooksByAverageRatingTask( ctx );
		new QueryBooksByBestRatingTask( ctx );
		new QueryBooksByNewestPublishedTask( ctx );
		new QueryBooksBySummaryTask( ctx );
		new QueryBooksByTitleTask( ctx );
		new QueryBooksByTotalSoldTask( ctx );
	}

}
