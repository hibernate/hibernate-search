/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.jsr352.massindexing.impl.util;

/**
 * Information about a target partition which can not be stored in the partition properties as String values. In
 * particular, the boundary properties help us to identify the lower boundary and upper boundary of a given partition,
 * with which the two ends of the scrollable results can be defined and be applied to
 * {@link org.hibernate.search.jsr352.internal.steps.lucene.ItemReader#open}.
 *
 * @author Mincong Huang
 */
public class PartitionBound {

	private Class<?> entityType;
	private Object lowerBound;
	private Object upperBound;

	public PartitionBound() {
	}

	public PartitionBound(Class<?> entityType) {
		this.entityType = entityType;
	}

	public PartitionBound(Class<?> entityType, Object lowerBound, Object upperBound) {
		this.entityType = entityType;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
	}

	public Class<?> getEntityType() {
		return entityType;
	}

	public String getEntityName() {
		return entityType.getName();
	}

	public Object getLowerBound() {
		return lowerBound;
	}

	public Object getUpperBound() {
		return upperBound;
	}

	public boolean isFirstPartition() {
		return lowerBound == null && upperBound != null;
	}

	public boolean isLastPartition() {
		return lowerBound != null && upperBound == null;
	}

	public boolean isUniquePartition() {
		return lowerBound == null && upperBound == null;
	}

	public void setEntityType(Class<?> entityType) {
		this.entityType = entityType;
	}

	@Override
	public String toString() {
		return "PartitionBound [entityType=" + entityType + ", lowerBound=" + lowerBound + ", upperBound=" + upperBound + "]";
	}
}
