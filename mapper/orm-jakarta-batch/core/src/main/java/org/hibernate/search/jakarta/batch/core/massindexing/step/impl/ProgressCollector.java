/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.impl;

import java.io.Serializable;

import jakarta.batch.api.partition.PartitionCollector;
import jakarta.batch.runtime.context.StepContext;
import jakarta.inject.Inject;

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
