/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.test.util;

import javax.batch.operations.JobOperator;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;

import org.jboss.logging.Logger;

/**
 * @author Yoann Rodiere
 */
public final class JobTestUtil {

	private static final Logger LOGGER = Logger.getLogger( JobTestUtil.class );

	private static final int THREAD_SLEEP = 1000;

	private JobTestUtil() {
	}

	public static JobExecution waitForTermination(JobOperator jobOperator, JobExecution jobExecution, int timeoutInMs)
			throws InterruptedException {
		long endTime = System.currentTimeMillis() + timeoutInMs;

		while ( !jobExecution.getBatchStatus().equals( BatchStatus.COMPLETED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.STOPPED )
				&& !jobExecution.getBatchStatus().equals( BatchStatus.FAILED )
				&& System.currentTimeMillis() < endTime ) {

			long executionId = jobExecution.getExecutionId();
			LOGGER.infof(
					"Job execution (id=%d) has status %s. Thread sleeps %d ms...",
					executionId,
					jobExecution.getBatchStatus(),
					THREAD_SLEEP );
			Thread.sleep( THREAD_SLEEP );
			jobExecution = jobOperator.getJobExecution( executionId );
		}

		return jobExecution;
	}

}
