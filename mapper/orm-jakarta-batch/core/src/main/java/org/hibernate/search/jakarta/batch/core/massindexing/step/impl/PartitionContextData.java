/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.impl;

import java.io.Serializable;

/**
 * Data model for each partition of step {@code produceLuceneDoc}. It contains a partition-level indexing progress.
 *
 * @author Gunnar Morling
 * @author Mincong Huang
 */
public class PartitionContextData implements Serializable {

	private static final long serialVersionUID = 1961574468720628080L;

	private PartitionProgress partitionProgress;

	public PartitionContextData(int partitionId, String entityName) {
		partitionProgress = new PartitionProgress( partitionId, entityName );
	}

	public void documentAdded(int increment) {
		partitionProgress.documentsAdded( increment );
	}

	public PartitionProgress getPartitionProgress() {
		return partitionProgress;
	}
}
