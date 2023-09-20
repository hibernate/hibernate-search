/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
