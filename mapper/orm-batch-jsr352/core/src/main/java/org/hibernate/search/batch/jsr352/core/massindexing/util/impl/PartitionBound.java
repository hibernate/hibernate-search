/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.batch.jsr352.core.massindexing.util.impl;

import org.hibernate.search.batch.jsr352.core.massindexing.step.impl.IndexScope;

/**
 * Information about a target partition which can not be stored in the partition properties as String values. In
 * particular, the boundary properties help us to identify the lower boundary and upper boundary of a given partition,
 * with which the two ends of the scrollable results can be defined and be applied to
 * {@link jakarta.batch.api.chunk.ItemReader#open}.
 *
 * @author Mincong Huang
 */
public class PartitionBound {

	private Class<?> entityType;
	private Object lowerBound;
	private Object upperBound;
	private IndexScope indexScope;

	public PartitionBound(Class<?> entityType, Object lowerBound, Object upperBound, IndexScope indexScope) {
		this.entityType = entityType;
		this.lowerBound = lowerBound;
		this.upperBound = upperBound;
		this.indexScope = indexScope;
	}

	public IndexScope getIndexScope() {
		return indexScope;
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

	public boolean hasUpperBound() {
		return upperBound != null;
	}

	public boolean hasLowerBound() {
		return lowerBound != null;
	}

	@Override
	public String toString() {
		return "PartitionBound [entityType=" + entityType + ", lowerBound=" + lowerBound + ", upperBound=" + upperBound
				+ ", indexScope=" + indexScope + "]";
	}
}
