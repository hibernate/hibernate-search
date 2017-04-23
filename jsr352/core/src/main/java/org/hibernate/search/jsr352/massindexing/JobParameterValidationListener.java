/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing;

import javax.batch.api.BatchProperty;
import javax.batch.api.listener.AbstractJobListener;
import javax.inject.Inject;

import org.hibernate.search.exception.SearchException;
import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import static org.hibernate.search.jsr352.massindexing.impl.util.ValidationUtil.validateCheckpointInterval;
import static org.hibernate.search.jsr352.massindexing.impl.util.ValidationUtil.validatePositive;

/**
 * Listener for job parameters validations before the start of a job execution.
 *
 * @author Mincong Huang
 */
public class JobParameterValidationListener extends AbstractJobListener {

	private static final Log log = LoggerFactory.make( Log.class );

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.CHECKPOINT_INTERVAL)
	private String checkpointIntervalStr;

	@Inject
	@BatchProperty(name = MassIndexingJobParameters.ROWS_PER_PARTITION)
	private String rowsPerPartitionStr;

	public JobParameterValidationListener() {
	}

	JobParameterValidationListener(String checkpointIntervalStr, String rowsPerPartitionStr) {
		this.checkpointIntervalStr = checkpointIntervalStr;
		this.rowsPerPartitionStr = rowsPerPartitionStr;
	}

	/**
	 * Validate job parameters.
	 *
	 * @throws SearchException if any validation fails.
	 */
	@Override
	public void beforeJob() throws SearchException {
		int checkpointInterval = parseInt( MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointIntervalStr );
		int rowsPerPartition = parseInt( MassIndexingJobParameters.ROWS_PER_PARTITION, rowsPerPartitionStr );

		validatePositive( MassIndexingJobParameters.CHECKPOINT_INTERVAL, checkpointInterval );
		validatePositive( MassIndexingJobParameters.ROWS_PER_PARTITION, rowsPerPartition );
		validateCheckpointInterval( checkpointInterval, rowsPerPartition );
	}

	private int parseInt(String parameterName, String parameterValue) {
		try {
			return Integer.parseInt( parameterValue );
		}
		catch (NumberFormatException e) {
			throw log.unableToParseJobParameter( parameterName, parameterValue, e );
		}
	}

}
