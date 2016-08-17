/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.internal.util;

/**
 * Information about a target partition which can not be stored in the partition
 * properties as String values. In particular, the boundary properties help us
 * to identify the lower boundary and upper boundary of a given partition, with
 * which the two ends of the scrollable results can be defined and be applied
 * to {@link org.hibernate.search.jsr352.internal.steps.lucene.ItemReader#open}.
 * 
 * @author Mincong Huang
 */
public class PartitionUnit {

	private Class<?> entityClazz;
	private Object lowerBound;
	private Object upperBound;
	private long rowsToIndex;

	public PartitionUnit() {

	}

	public PartitionUnit(Class<?> entityClazz, long rowsToIndex) {
		this.entityClazz = entityClazz;
		this.rowsToIndex = rowsToIndex;
	}

	public PartitionUnit(Class<?> entityClazz,
			long rowsToIndex,
			Object lowerBound,
			Object upperBound) {
		this.entityClazz = entityClazz;
		this.rowsToIndex = rowsToIndex;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Class<?> getEntityClazz() {
		return entityClazz;
	}

	public String getEntityName() {
		return entityClazz.toString();
	}

	public long getRowsToIndex() {
		return rowsToIndex;
	}

	public Object getLowerBound() {
		return lowerBound;
	}

	public Object getUpperBound() {
		return upperBound;
	}

	public boolean isFirstPartition() {
		boolean isFirst = false;
		if ( lowerBound == null && upperBound != null ) {
			isFirst = true;
		}
		return isFirst;
	}

	public boolean isLastPartition() {
		boolean isLast = false;
		if ( lowerBound != null && upperBound == null ) {
			isLast = true;
		}
		return isLast;
	}

	public boolean isUniquePartition() {
		boolean isUnique = false;
		if ( lowerBound == null && upperBound == null ) {
			isUnique = true;
		}
		return isUnique;
	}

	public void setEntityClazz(Class<?> entityClazz) {
		this.entityClazz = entityClazz;
	}

	public void setRowsToIndex(long rowsToIndex) {
		this.rowsToIndex = rowsToIndex;
	}

	@Override
	public String toString() {
		return "PartitionUnit [entityClazz=" + entityClazz + ", lowerBound="
				+ lowerBound + ", upperBound=" + upperBound + ", rowsToIndex="
				+ rowsToIndex + "]";
	}
}
