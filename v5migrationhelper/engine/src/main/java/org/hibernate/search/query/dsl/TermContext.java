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
public interface TermContext extends QueryCustomization<TermContext> {
	/**
	 * @param field The field name the term query is executed on
	 *
	 * @return {@code TermMatchingContext} to continue the term query
	 */
	TermMatchingContext onField(String field);

	/**
	 * @param field The field names the term query is executed on. The underlying properties for the specified
	 * fields need to be of the same type. For example, it is not possible to use this method with a mixture of
	 * string and date properties. In the mixed case an alternative is to build multiple term queries and combine them
	 * via {@link QueryBuilder#bool()}
	 * @return {@code TermMatchingContext} to continue the term query
	 */
	TermMatchingContext onFields(String... field);

	/**
	 * Use a fuzzy search approximation (aka edit distance)
	 *
	 * @return {@code FuzzyContext} to continue the fuzzy query
	 */
	FuzzyContext fuzzy();

	/**
	 * Treat the query as a wildcard query which means:
	 * <ul>
	 * <li> '?' represents any single character</li>
	 * <li> '*' represents any character sequence </li>
	 * </ul>
	 * For faster results, it is recommended that the query text does not
	 * start with '?' or '*'.
	 *
	 * @return {@code WildcardContext} to continue the wildcard query
	 */
	WildcardContext wildcard();
}
