/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * Represents the context in which a must clause is described.
 *
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface MustJunction extends BooleanJunction<MustJunction> {
	/**
	 * Negate the must clause.
	 * Results of the boolean query do NOT match the subquery.
	 * A negated must clause always disables scoring on the subquery.
	 *
	 * @return the same {@link BooleanJunction} for method chaining.
	 */
	BooleanJunction not();

	/**
	 * Disables scoring on the subquery.
	 * If you are only interested to use this clause as a filtering criteria
	 * and don't need it to affect result scoring, this might improve performance.
	 * @return the same {@link BooleanJunction} for method chaining.
	 */
	BooleanJunction disableScoring();
}
