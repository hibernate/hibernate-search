/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * An object where the <a href="#disjuncts">disjuncts</a> of a
 * {@link TypedSearchPredicateFactory#disjunctionMax() disjunction max predicate} can be added.
 *
 * <h2 id="disjuncts">Disjuncts</h2>
 * <p>
 * Documents will have to match <em>any</em> disjunct, exactly as with
 * {@link TypedSearchPredicateFactory#or() or}. The difference is in the score: where
 * {@code or} sums the scores of every matching clause, a disjunction max predicate takes the
 * <em>highest</em> score among the matching disjuncts, and only adds a configurable fraction of the
 * others through the {@link DisjunctionMaxPredicateOptionsStep#tieBreaker(float) tie breaker}.
 * <p>
 * This is the behavior to prefer when the disjuncts are alternative representations of the
 * <em>same</em> user intent — most commonly the same text matched against several fields — because
 * summing their scores would reward documents that happen to repeat the term in many fields.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this collector).
 */
public interface DisjunctionMaxPredicateClausesCollector<
		SR,
		S extends DisjunctionMaxPredicateClausesCollector<SR, ?>> {

	/**
	 * Adds the specified predicate as a <a href="#disjuncts">disjunct</a>.
	 *
	 * @param searchPredicate The predicate to add.
	 * @return {@code this}, for method chaining.
	 */
	S add(PredicateFinalStep searchPredicate);

	/**
	 * Adds the specified previously-built {@link SearchPredicate} as a
	 * <a href="#disjuncts">disjunct</a>.
	 *
	 * @param searchPredicate The predicate to add.
	 * @return {@code this}, for method chaining.
	 */
	S add(SearchPredicate searchPredicate);

	/**
	 * Adds a <a href="#disjuncts">disjunct</a> to be defined by the given function.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * @return {@code this}, for method chaining.
	 */
	S add(Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Delegates setting the <a href="#disjuncts">disjuncts</a> to a given consumer.
	 *
	 * @param contributor A consumer that will add disjuncts to the collector passed in parameter.
	 * @return {@code this}, for method chaining.
	 */
	S with(Consumer<? super S> contributor);

	/**
	 * @return {@code true} if at least one <a href="#disjuncts">disjunct</a> was added.
	 */
	boolean hasClause();
}
