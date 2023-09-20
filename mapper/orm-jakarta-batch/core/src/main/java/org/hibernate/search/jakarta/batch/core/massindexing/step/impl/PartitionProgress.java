/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jakarta.batch.core.massindexing.step.impl;

import java.io.Serializable;

/**
 * @author Mincong Huang
 */
public class PartitionProgress implements Serializable {

	private static final long serialVersionUID = -5923539799807235429L;
	private String entityName;
	private int partitionId;
	private long workDone;

	public PartitionProgress(int partitionId, String entityName) {
		this.partitionId = partitionId;
		this.entityName = entityName;
		this.workDone = 0L;
	}

	/**
	 * documentsAdded is an elementary count. It records how many items have been written in the current chunk. This
	 * value is overwritten be the item writer at the end of each
	 * {@link EntityWriter#writeItems}
	 */
	public void documentsAdded(int increment) {
		this.workDone += increment;
	}

	public String getEntityName() {
		return entityName;
	}

	public int getPartitionId() {
		return partitionId;
	}

	public long getWorkDone() {
		return workDone;
	}

	@Override
	public String toString() {
		return "PartitionProgress [workDone=" + workDone + ", entityName=" + entityName
				+ ", partitionId=" + partitionId + "]";
	}
}
