/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

import org.apache.lucene.search.Query;

/**
 * Operations common to all types of queries
 *
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface QueryCustomization<T> {

	/**
	 * Boost the query to a given value
	 * Most of the time positive float:
	 *  - lower than 1 to diminish the weight
	 *  - higher than 1 to increase the weight
	 *
	 * Could be negative but not unless you understand what is going on (advanced)
	 * @param boost the value to use as boost
	 * @return an instance of T for method chaining
	 */
	T boostedTo(float boost);

	/**
	 * All results matching the query have a constant score equals to the boost
	 * FIXME is that true?
	 * @return an instance of T for method chaining
	 */
	T withConstantScore();

	/**
	 * Filter the query results with the given Query instance
	 * @param filter the Query to use as a filter
	 * @return an instance of T for method chaining
	 */
	T filteredBy(Query filter);

}
