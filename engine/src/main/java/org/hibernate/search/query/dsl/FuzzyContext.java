/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.query.dsl;

/**
 * @author Emmanuel Bernard
 */
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
	 * Threshold above which two terms are considered similar enough.
	 * Value between 0 and 1 (1 excluded)
	 * Defaults to .5
	 *
	 * @deprecated use {@link #withEditDistanceUpTo(int)}
	 * @param threshold the value for the threshold
	 * @return a {@link FuzzyContext}
	 */
	@Deprecated
	FuzzyContext withThreshold(float threshold);

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
