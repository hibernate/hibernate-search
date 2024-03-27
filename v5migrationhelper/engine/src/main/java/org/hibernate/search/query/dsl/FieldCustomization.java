/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 * @param <T> the type of customization required for the field
 * @deprecated See the deprecation note on {@link QueryBuilder}.
 */
@Deprecated
public interface FieldCustomization<T> {
	/**
	 * Boost the field to a given value
	 * Most of the time positive float:
	 *  - lower than 1 to diminish the weight
	 *  - higher than 1 to increase the weight
	 *
	 * Could be negative but not unless you understand what is going on (advanced)
	 * @param boost the boost value, it can be negative (advance)
	 * @return an instance of T for method chaining
	 */
	T boostedTo(float boost);

	/**
	 * Advanced
	 * Do not execute the analyzer on the text.
	 * (It is usually a good idea to apply the analyzer)
	 * @return an instance of T for method chaining
	 */
	T ignoreAnalyzer();

	/**
	 * Do not try and find the field bridge nor apply the object / string conversion
	 * matching objects should be of type String in this case.
	 * @return an instance of T for method chaining
	 */
	T ignoreFieldBridge();
}
