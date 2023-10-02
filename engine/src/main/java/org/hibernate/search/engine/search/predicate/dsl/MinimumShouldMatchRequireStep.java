/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "minimum should match" constraint definition
 * where the minimum required number or percentage of should clauses to match can be set.
 * <p>
 * See <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraints</a>.
 *
 * @param <N> The type of the next step (returned by {@link MinimumShouldMatchMoreStep#end()}).
 */
public interface MinimumShouldMatchRequireStep<N> {

	/**
	 * Add a <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return The next step.
	 */
	MinimumShouldMatchMoreStep<N> thenRequireNumber(int matchingClausesNumber);

	/**
	 * Add a <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * See <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return The next step.
	 */
	MinimumShouldMatchMoreStep<N> thenRequirePercent(int matchingClausesPercent);

}
