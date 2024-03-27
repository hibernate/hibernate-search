/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * @author Guillaume Smet
 * @deprecated See the deprecation note on {@link QueryBuilder}.
*/
@Deprecated
public interface SimpleQueryStringMatchingContext extends SimpleQueryStringDefinitionTermination {
	/**
	 * Boost the field to a given value
	 * Most of the time positive float:
	 *  - lower than 1 to diminish the weight
	 *  - higher than 1 to increase the weight
	 *
	 * Could be negative but not unless you understand what is going on (advanced)
	 * @param boost the value to use as boost
	 * @return {@code this} for method chaining
	 */
	SimpleQueryStringMatchingContext boostedTo(float boost);

	/**
	 * Field the query is executed on.
	 * @param field The absolute path of an index field.
	 * @return {@code this} for method chaining
	 */
	SimpleQueryStringMatchingContext andField(String field);

	/**
	 * Fields the query is executed on.
	 * @param field The absolute path of an index field.
	 * @return {@code this} for method chaining
	 */
	SimpleQueryStringMatchingContext andFields(String... field);

	/**
	 * Define the default operator as AND.
	 * @return {@code this} for method chaining
	 */
	SimpleQueryStringDefinitionTermination withAndAsDefaultOperator();

}
