/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query.dsl.sort;

/**
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @deprecated See the deprecation note on {@link SortContext}.
 */
@Deprecated
public interface SortOrder<T> {
	/**
	 * Sort in ascending order.
	 * @return {@code this} for method chaining
	 */
	T asc();

	/**
	 * Sort in descending order.
	 * @return {@code this} for method chaining
	 */
	T desc();
}
