/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The step in a "nested" predicate definition where the predicate to nest can be set.
 *
 * @param <N> The type of the next step.
 * @deprecated Use {@link SearchPredicateFactory#nested(String)} instead.
 */
@Deprecated
public interface NestedPredicateNestStep<N extends NestedPredicateOptionsStep<?>> {

	/**
	 * Set the inner predicate to a previously-built {@link SearchPredicate}.
	 * <p>
	 * Matching documents are those for which at least one element of the nested object field
	 * matches the inner predicate.
	 *
	 * @param searchPredicate The predicate that must be matched by at least one element of the nested object field.
	 * @return The next step.
	 */
	N nest(SearchPredicate searchPredicate);

	/*
	 * Syntactic sugar allowing to skip the toPredicate() call by passing a PredicateFinalStep
	 * directly.
	 */

	/**
	 * Set the inner predicate to an almost-built {@link SearchPredicate}.
	 * <p>
	 * Matching documents are those for which at least one element of the nested object field
	 * matches the inner predicate.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return The next step.
	 */
	default N nest(PredicateFinalStep dslFinalStep) {
		return nest( dslFinalStep.toPredicate() );
	}

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing the structure of the predicate building code to mirror the structure of predicates,
	 * even for complex predicate building requiring for example if/else statements.
	 */

	/**
	 * Set the inner predicate defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 * <p>
	 * Matching documents are those for which at least one element of the nested object field
	 * matches the inner predicate.
	 *
	 * @param predicateContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return The next step.
	 */
	N nest(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor);

}
