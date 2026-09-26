/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "disjunction max" predicate definition where options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface DisjunctionMaxPredicateOptionsStep<S extends DisjunctionMaxPredicateOptionsStep<?>>
		extends PredicateScoreStep<S>, PredicateFinalStep {

	/**
	 * Sets the multiplier applied to the scores of the disjuncts that are not the highest scoring one.
	 * <p>
	 * With the default value of {@code 0}, only the highest score among the matching
	 * <a href="DisjunctionMaxPredicateClausesCollector.html#disjuncts">disjuncts</a> contributes to the
	 * score of the document. With a value of {@code 1}, every matching disjunct contributes its full
	 * score, which makes the predicate equivalent to {@link TypedSearchPredicateFactory#or() or}.
	 * Values in between let documents matching several disjuncts rank slightly higher than documents
	 * matching only one, without letting the number of matches dominate the score.
	 *
	 * @param tieBreaker The tie breaker. Values between {@code 0} and {@code 1} are the useful range;
	 * anything outside it is for experts only.
	 * @return {@code this}, for method chaining.
	 */
	S tieBreaker(float tieBreaker);
}
