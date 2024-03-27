/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "match" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface MatchPredicateOptionsStep<S extends MatchPredicateOptionsStep<?>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

	/**
	 * Enable fuzziness for this match predicate; only works for text fields.
	 * <p>
	 * Fuzziness allows to match documents that do not contain the value to match,
	 * but a close value, for example with one letter that differs.
	 *
	 * @return {@code this}, for method chaining.
	 * @see #fuzzy(int, int)
	 */
	default S fuzzy() {
		return fuzzy( 2, 0 );
	}

	/**
	 * Enable fuzziness for this match predicate; only works for text fields.
	 * <p>
	 * Fuzziness allows to match documents that do not contain the value to match,
	 * but a close value, for example with one letter that differs.
	 *
	 * @param maxEditDistance The maximum value of the edit distance, which defines how permissive the fuzzy predicate will be.
	 * @return {@code this}, for method chaining.
	 * @see #fuzzy(int, int)
	 */
	default S fuzzy(int maxEditDistance) {
		return fuzzy( maxEditDistance, 0 );
	}

	/**
	 * Enable fuzziness for this match predicate; only works for text fields.
	 * <p>
	 * Fuzziness allows to match documents that do not contain the value to match,
	 * but a close value, for example with one letter that differs.
	 *
	 * @param maxEditDistance The maximum value of the edit distance, which defines how permissive the fuzzy predicate will be.
	 * <p>
	 * Roughly speaking, the edit distance is the number of changes between two terms: switching characters, removing them, ...
	 * <p>
	 * If zero, then fuzziness is completely disabled.
	 * The other accepted values, {@code 1} and {@code 2}, are increasingly fuzzy.
	 * @param exactPrefixLength Length of the prefix that has to match exactly, i.e. for which fuzziness will not be allowed.
	 * <p>
	 * A non-zero value is recommended if the index contains a large amount of distinct terms.
	 * @return {@code this}, for method chaining.
	 */
	S fuzzy(int maxEditDistance, int exactPrefixLength);

	/**
	 * Define an analyzer to use at query time to interpret the value to match.
	 * <p>
	 * If this method is not called, the analyzer defined on the field will be used.
	 *
	 * @param analyzerName The name of the analyzer to use in the query for this predicate.
	 * @return {@code this}, for method chaining.
	 */
	S analyzer(String analyzerName);

	/**
	 * Any analyzer and normalizer defined on any field will be ignored to interpret the value to match.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S skipAnalysis();
}
