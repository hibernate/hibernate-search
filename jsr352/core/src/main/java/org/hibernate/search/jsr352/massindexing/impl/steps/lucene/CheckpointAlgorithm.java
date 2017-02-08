/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.steps.lucene;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractCheckpointAlgorithm;
import javax.batch.runtime.Metric;
import javax.batch.runtime.context.StepContext;
import javax.inject.Inject;

/**
 * This checkpoint algorithm is used to provide a checkpoint decision based on the item count N given by the user. So,
 * the job is ready to checkpoint each N items. If user does not specify the itemCount value, default value described in
 * the mass indexer will be applied.
 *
 * @author Mincong Huang
 */
public class CheckpointAlgorithm extends AbstractCheckpointAlgorithm {

	@Inject
	private StepContext stepContext;

	@Inject
	@BatchProperty
	private String itemCount;

	@Override
	public boolean isReadyToCheckpoint() throws Exception {
		Metric[] metrics = stepContext.getMetrics();
		for ( final Metric m : metrics ) {
			if ( m.getType().equals( Metric.MetricType.READ_COUNT ) ) {
				return m.getValue() % Integer.parseInt( itemCount ) == 0;
			}
		}
		throw new Exception( "Metric READ_COUNT not found" );
	}
}
