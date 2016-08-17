/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.steps.lucene;

import java.io.Serializable;

/**
 * @author Mincong Huang
 */
public class PartitionProgress implements Serializable {

	private static final long serialVersionUID = -5923539799807235429L;
	private long workDone = 0L;
	private String entityName;
	private int partitionID;
	private boolean isRestarted = false;

	public PartitionProgress(int partitionID, String entityName) {
		this.partitionID = partitionID;
		this.entityName = entityName;
	}

	/**
	 * documentsAdded is an elementary count. It records
	 * how many items have been written in the current chunk. This value is
	 * overwritten be the item writer at the end of each
	 * {@link org.hibernate.search.jsr352.internal.steps.lucene.ItemWriter#writeItems}
	 */
	public void documentsAdded(long increment) {
		this.workDone += increment;
	}

	public String getEntityName() {
		return entityName;
	}

	public void setEntityName(String entityName) {
		this.entityName = entityName;
	}

	public int getPartitionID() {
		return partitionID;
	}

	public void setPartitionID(int partitionID) {
		this.partitionID = partitionID;
	}

	public boolean isRestarted() {
		return isRestarted;
	}
	
	public void setRestarted(boolean isRestarted) {
		this.isRestarted = isRestarted;
	}

	public long getWorkDone() {
		return workDone;
	}

	public void setWorkDone(long workDone) {
		this.workDone = workDone;
	}

	@Override
	public String toString() {
		return "PartitionProgress [workDone=" + workDone + ", entityName=" + entityName
				+ ", partitionID=" + partitionID + "]";
	}
}
