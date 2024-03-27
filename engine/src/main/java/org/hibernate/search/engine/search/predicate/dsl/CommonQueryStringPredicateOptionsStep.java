/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.common.BooleanOperator;

/**
 * The final step in a query string predicate definition, where optional parameters can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface CommonQueryStringPredicateOptionsStep<S extends CommonQueryStringPredicateOptionsStep<?>>
		extends PredicateFinalStep, PredicateScoreStep<S> {

	/**
	 * Define the default operator.
	 * <p>
	 * By default, unless the query string contains explicit operators,
	 * documents will match if <em>any</em> term mentioned in the query string is present in the document ({@code OR} operator).
	 * This can be used to change the default behavior to {@code AND},
	 * making document match if <em>all</em> terms mentioned in the query string are present in the document.
	 *
	 * @param operator The default operator ({@code OR} or {@code AND}).
	 * @return {@code this}, for method chaining.
	 */
	S defaultOperator(BooleanOperator operator);

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

	/**
	 * Add a default <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return {@code this}, for method chaining.
	 */
	default S minimumShouldMatchNumber(int matchingClausesNumber) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequireNumber( matchingClausesNumber )
				.end();
	}

	/**
	 * Add a default <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * See <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return {@code this}, for method chaining.
	 */
	default S minimumShouldMatchPercent(int matchingClausesPercent) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequirePercent( matchingClausesPercent )
				.end();
	}

	MinimumShouldMatchConditionStep<? extends S> minimumShouldMatch();

	/**
	 * Start defining the minimum number of "should" constraints that have to match
	 * in order for the boolean predicate to match.
	 * <p>
	 * See {@link MinimumShouldMatchConditionStep}.
	 *
	 * @return A {@link MinimumShouldMatchConditionStep} where constraints can be defined.
	 */
	S minimumShouldMatch(
			Consumer<? super MinimumShouldMatchConditionStep<?>> constraintContributor);

}
