/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;

/**
 * The step in a query definition where the predicate, i.e. the "WHERE" clause, can be set.
 *
 * @param <N> The type of the next step, returned after a predicate is defined.
 * @param <H> The type of hits for the created query.
 * @param <PDF> The type of factory used to create predicates in {@link #where(Function)}.
 * @param <LOS> The type of the initial step of the loading options definition DSL accessible through {@link SearchQueryOptionsStep#loading(Consumer)}.
 */
public interface SearchQueryWhereStep<
		N extends SearchQueryOptionsStep<?, H, LOS, ?, ?>,
		H,
		LOS,
		PDF extends SearchPredicateFactory> {

	/**
	 * Set the predicate for this query.
	 * @param predicate A {@link SearchPredicate} object obtained from the search scope.
	 * @return The next step.
	 */
	N where(SearchPredicate predicate);

	/**
	 * Set the predicate for this query.
	 * @param predicateContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return The next step.
	 */
	N where(Function<? super PDF, ? extends PredicateFinalStep> predicateContributor);

	/**
	 * Set the predicate for this query.
	 * @param predicateContributor A consumer that will use the factory passed in parameter to create predicates
	 * and add them as clauses to the collector passed in parameter.
	 * Should generally be a lambda expression.
	 * The resulting root predicate will have to match <em>all</em> clauses.
	 * @return The next step.
	 * @see SimpleBooleanPredicateClausesCollector
	 */
	N where(BiConsumer<? super PDF, ? super SimpleBooleanPredicateClausesCollector<?>> predicateContributor);

}
