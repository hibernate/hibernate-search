/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

/**
 * A superinterface for contexts allowing to define a sort order.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortOrderContext<T> {
	/**
	 * Sort in ascending order.
	 */
	default T asc() {
		return order( SortOrder.ASC );
	}

	/**
	 * Sort in descending order.
	 */
	default T desc() {
		return order( SortOrder.DESC );
	}

	T order(SortOrder order);
}
