/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import java.io.Serializable;

import javax.batch.api.partition.PartitionCollector;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 * Progress collectors run on the partitioned step threads and there's one collector per partition. They collect the
 * partition progress and send it to the partition analyzer.
 *
 * @author Mincong Huang
 */
public class ProgressCollector implements PartitionCollector {

	@Inject
	private StepContext stepContext;

	/**
	 * The collectPartitionData method receives control periodically during partition processing. This method receives
	 * control on each thread processing a partition as lucene document production, once at the end of the process.
	 */
	@Override
	public Serializable collectPartitionData() throws Exception {
		return ( (PartitionContextData) stepContext.getTransientUserData() )
				.getPartitionProgress();
	}
}
