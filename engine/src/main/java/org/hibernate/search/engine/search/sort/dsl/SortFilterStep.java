/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.sort.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;

/**
 * The step in a sort definition where a filter can be set
 * to select nested objects from which values will be extracted for this sort.
 *
 * @param <S> The "self" type (the actual exposed type of this step)
 * @param <PDF> The type of factory used to create predicates in {@link #filter(Function)}.
 */
public interface SortFilterStep<S, PDF extends SearchPredicateFactory> {

	/**
	 * Filter nested objects from which values will be extracted for this sort.
	 * <p>
	 * The filter is based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	S filter(SearchPredicate searchPredicate);

	/**
	 * Filter nested objects from which values will be extracted for this sort.
	 * <p>
	 * The filter is defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	S filter(Function<? super PDF, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Filter nested objects from which values will be extracted for this sort.
	 * <p>
	 * The filter is based on an almost-built {@link SearchPredicate}.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default S filter(PredicateFinalStep dslFinalStep) {
		return filter( dslFinalStep.toPredicate() );
	}


}
