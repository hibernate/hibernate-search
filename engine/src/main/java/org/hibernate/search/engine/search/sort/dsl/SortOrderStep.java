/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
