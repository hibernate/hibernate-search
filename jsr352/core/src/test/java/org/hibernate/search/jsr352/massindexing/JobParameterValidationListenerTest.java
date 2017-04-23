/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import org.hibernate.search.exception.SearchException;

import org.junit.Test;

/**
 * @author Mincong Huang
 */
public class JobParameterValidationListenerTest {

	private JobParameterValidationListener validationListener;

	@Test(expected = SearchException.class)
	public void validateRowsPerPartition_valueIsNegative() throws Exception {
		String checkpointIntervalStr = "100";
		String rowsPerPartitionStr = "-1";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
	}

	@Test(expected = SearchException.class)
	public void validateRowsPerPartition_valueIsZero() throws Exception {
		String checkpointIntervalStr = "100";
		String rowsPerPartitionStr = "0";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_valueIsNegative() throws Exception {
		String checkpointIntervalStr = "-1";
		String rowsPerPartitionStr = "100";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_valueIsZero() throws Exception {
		String checkpointIntervalStr = "0";
		String rowsPerPartitionStr = "100";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_greaterThanRowsPerPartitions() throws Exception {
		String checkpointIntervalStr = "101";
		String rowsPerPartitionStr = "100";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
	}

	@Test(expected = SearchException.class)
	public void validateCheckpointInterval_equalToRowsPerPartitions() throws Exception {
		String checkpointIntervalStr = "100";
		String rowsPerPartitionStr = "100";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
	}

	@Test
	public void validateCheckpointInterval_lessThanRowsPerPartitions() throws Exception {
		String checkpointIntervalStr = "99";
		String rowsPerPartitionStr = "100";
		validationListener = new JobParameterValidationListener( checkpointIntervalStr, rowsPerPartitionStr );
		validationListener.beforeJob();
		// ok, no exception
	}

}
