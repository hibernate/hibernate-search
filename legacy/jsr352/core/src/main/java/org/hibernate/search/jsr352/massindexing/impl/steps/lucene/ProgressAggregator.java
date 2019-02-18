/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.Serializable;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import javax.batch.api.partition.AbstractPartitionAnalyzer;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

import org.hibernate.search.jsr352.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import java.lang.invoke.MethodHandles;

/**
 * Progress aggregator aggregates the intermediary chunk progress received from each partition sent via the collectors.
 * It runs on the step main thread.
 *
 * @author Mincong Huang
 */
public class ProgressAggregator extends AbstractPartitionAnalyzer {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Inject
	private StepContext stepContext;

	/**
	 * Analyze data obtained from different partition plans via partition data collectors. The current analyze is to
	 * summarize to their progresses : workDone = workDone1 + workDone2 + ... + workDoneN. Then it displays the total
	 * mass index progress in percentage. This method is very similar to the current simple progress monitor.
	 *
	 * @param fromCollector the indexing progress of one partition, obtained from partition collector's method
	 * #collectPartitionData()
	 */
	@Override
	public void analyzeCollectorData(Serializable fromCollector) throws Exception {
		// update step-level progress using partition-level progress
		PartitionProgress partitionProgress = (PartitionProgress) fromCollector;
		StepProgress stepProgress = (StepProgress) stepContext.getTransientUserData();
		stepProgress.updateProgress( partitionProgress );

		// logging
		StringBuilder sb = new StringBuilder( System.lineSeparator() );
		formatEntityProgresses( stepProgress ).forEach( (msg) -> {
			sb.append( System.lineSeparator() ).append( "\t" ).append( msg );
		} );
		sb.append( System.lineSeparator() );
		log.analyzeIndexProgress( sb.toString() );
	}

	private Stream<String> formatEntityProgresses(StepProgress stepProgress) {
		Map<String, Long> entityProgress = stepProgress.getEntityProgress();
		return stepProgress.getEntityTotal().entrySet().stream()
				.map( (entry) ->
					formatEntityProgress( entry.getKey() , entityProgress.get( entry.getKey() ), entry.getValue() )
				);
	}

	private String formatEntityProgress(String entity, Long processed, Long total) {
		if ( total == null ) {
			// Total number of entities unknown
			return String.format(
					Locale.ROOT,
					"%s: %d entities processed.",
					entity,
					processed
			);
		}
		else {
			return String.format(
					Locale.ROOT,
					"%s: %d/%d entities processed (%.2f%%).",
					entity,
					processed,
					total,
					processed * 100F / total
			);
		}
	}
}
