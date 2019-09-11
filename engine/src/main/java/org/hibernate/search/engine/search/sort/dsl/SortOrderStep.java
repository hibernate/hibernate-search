/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

/**
 * The step in a sort definition where the order can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortOrderStep<S> {
	/**
	 * Sort in ascending order.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S asc() {
		return order( SortOrder.ASC );
	}

	/**
	 * Sort in descending order.
	 *
	 * @return {@code this}, for method chaining.
	 */
	default S desc() {
		return order( SortOrder.DESC );
	}

	/**
	 * Sort in the given order.
	 *
	 * @param order The order.
	 * @return {@code this}, for method chaining.
	 */
	S order(SortOrder order);
}
