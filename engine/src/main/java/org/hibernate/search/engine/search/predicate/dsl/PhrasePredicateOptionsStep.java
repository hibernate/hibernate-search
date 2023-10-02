/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The final step in a "phrase" predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface PhrasePredicateOptionsStep<S extends PhrasePredicateOptionsStep<?>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

	/**
	 * Sets the slop, which defines how permissive the phrase predicate will be.
	 * <p>
	 * If zero (the default), then the predicate will only match the exact phrase given (after analysis).
	 * Higher values are increasingly permissive, allowing unexpected words or words that switched position.
	 * <p>
	 * The slop represents the number of edit operations that can be applied to the phrase to match,
	 * where each edit operation moves one word by one position.
	 * So {@code quick fox} with a slop of 1 can become {@code quick <word> fox}, where {@code <word>} can be any word.
	 * {@code quick fox} with a slop of 2 can become {@code quick <word> fox}, or {@code quick <word1> <word2> fox}
	 * or even {@code fox quick} (two operations: moved {@code fox} to the left and {@code quick} to the right).
	 * And similarly for higher slops and for phrases with more words.
	 *
	 * @param slop The slop value.
	 * @return {@code this}, for method chaining.
	 */
	S slop(int slop);

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
	 * Any analyzer or normalizer defined on any field will be ignored to interpret the value to match.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S skipAnalysis();

}
