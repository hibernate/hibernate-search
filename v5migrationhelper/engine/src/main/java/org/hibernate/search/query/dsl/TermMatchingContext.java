/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */

package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 * @deprecated See the deprecation note on {@link QueryBuilder}.
*/
@Deprecated
public interface TermMatchingContext extends FieldCustomization<TermMatchingContext> {
	/**
	 * Value searched in the field or fields.
	 * The value is passed to the field's:
	 *  - field bridge
	 *  - analyzer (unless ignoreAnalyzer is called).
	 *
	 * @param value the value to match
	 * @return {@code this} for method chaining
	 */
	TermTermination matching(Object value);

	/**
	 * field / property the term query is executed on
	 *
	 * @param field another field involved in the query
	 * @return {@code this} for method chaining
	 */
	TermMatchingContext andField(String field);
}
