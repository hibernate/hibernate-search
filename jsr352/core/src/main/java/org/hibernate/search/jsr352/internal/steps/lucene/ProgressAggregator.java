/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.JobContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.hibernate.search.jsr352.internal.JobContextData;
import org.jboss.logging.Logger;

/**
 * Progress aggregator aggregates the intermediary chunk progress received from
 * each partition sent via the collectors. It runs on the step main thread.
 *
 * @author Mincong Huang
 */
@Named
public class ProgressAggregator extends AbstractPartitionAnalyzer {

	private static final Logger LOGGER = Logger.getLogger( ProgressAggregator.class );
	private final JobContext jobContext;
	private Map<Integer, Long> globalProgress = new HashMap<>();

	@Inject
	public ProgressAggregator(JobContext jobContext) {
		this.jobContext = jobContext;
	}

	/**
	 * Analyze data obtained from different partition plans via partition data
	 * collectors. The current analyze is to summarize to their progresses :
	 * workDone = workDone1 + workDone2 + ... + workDoneN. Then it displays the
	 * total mass index progress in percentage. This method is very similar to
	 * the current simple progress monitor.
	 * 
	 * @param fromCollector the count of finished work of one partition,
	 * obtained from partition collector's method #collectPartitionData()
	 */
	@Override
	public void analyzeCollectorData(Serializable fromCollector) throws Exception {

		// receive progress update from partition
		PartitionProgress progress = (PartitionProgress) fromCollector;
		int PID = progress.getPartitionID();
		long done = progress.getWorkDone();
		globalProgress.put( PID, done );

		// compute the global progress.
		JobContextData jobData = (JobContextData) jobContext.getTransientUserData();
		long totalTodo = jobData.getTotalEntityToIndex();
		long totalDone = globalProgress.values()
				.stream()
				.mapToLong( Long::longValue )
				.sum();
		String comment = "";
		if ( !progress.isRestarted() ) {
			if ( totalTodo != 0 ) {
				comment = String.format( "%.1f%%", 100F * totalDone / totalTodo );
			}
			else {
				comment = "??.?%";
			}
		}
		else {
			// TODO Currently, percentage is not supported for the restarted job
			// instance, because collected data is lost after the job stop. The
			// checkpoint mechanism is only available for partition scope and
			// JSR 352 doesn't provide any API to store step-scope data.
			comment = "restarted";
		}
		LOGGER.infof( "%d works processed (%s).", totalDone, comment );
	}
}
