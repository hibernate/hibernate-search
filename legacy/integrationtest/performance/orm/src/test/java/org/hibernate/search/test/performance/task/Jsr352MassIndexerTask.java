/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.test.performance.task;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.JobExecution;

import org.hibernate.search.FullTextSession;
import org.hibernate.search.jsr352.massindexing.MassIndexingJob;
import org.hibernate.search.test.performance.model.Book;
import org.hibernate.search.test.performance.scenario.TestScenarioContext;

public class Jsr352MassIndexerTask extends AbstractTask {
	public Jsr352MassIndexerTask(TestScenarioContext ctx) {
		super( ctx );
	}

	@Override
	protected void execute(FullTextSession fts) {
		JobOperator operator = BatchRuntime.getJobOperator();
		try {
			long executionId = operator.start(
					MassIndexingJob.NAME,
					MassIndexingJob.parameters()
							.forEntity( Book.class )
							// Match the defaults of the Session mass indexer: 6 document builders + 1 ID loader
							.maxThreads( 6 + 1 )
							.build()
			);
			JobExecution execution = operator.getJobExecution( executionId );
			while ( true ) {
				switch ( execution.getBatchStatus() ) {
					case STARTING:
					case STARTED:
					case STOPPING:
						Thread.sleep( 100 );
						break;
					case COMPLETED:
						return;
					case STOPPED:
					case FAILED:
					case ABANDONED:
						throw new RuntimeException( "Indexing failed with status: " + execution.getExitStatus() );
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException( e );
		}
	}
}
