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
public interface FuzzyContext extends QueryCustomization<FuzzyContext> {
	/**
	 * field / property the term query is executed on
	 * @param field the name of the field
	 * @return a {@link TermMatchingContext}
	 */
	TermMatchingContext onField(String field);

	/**
	 * fields / properties the term query is executed on
	 * @param fields the names of the fields
	 * @return a {@link TermMatchingContext}
	 */
	TermMatchingContext onFields(String... fields);

	/**
	 * Maximum value of the edit distance. Roughly speaking, the number of changes between two terms to be considered
	 * close enough.
	 * Can be either 1 or 2 (0 would mean no fuzziness).
	 *
	 * Defaults to 2.
	 * @param maxEditDistance max value for the edit distance
	 * @return a {@link FuzzyContext}
	 */
	FuzzyContext withEditDistanceUpTo(int maxEditDistance);

	/**
	 * Size of the prefix ignored by the fuzzyness.
	 * A non zero value is recommended if the index contains a huge amount of distinct terms
	 *
	 * Defaults to 0
	 * @param prefixLength the length of the prefix
	 * @return a {@link FuzzyContext}
	 */
	FuzzyContext withPrefixLength(int prefixLength);
}
