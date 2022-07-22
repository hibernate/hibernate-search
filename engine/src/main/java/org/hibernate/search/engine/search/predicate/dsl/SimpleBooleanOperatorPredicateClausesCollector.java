/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * A generic superinterface for "simple predicate" DSL steps that involve collecting clauses.
 * <p>
 * See also {@link PredicateScoreStep} or {@link PredicateFinalStep}.
 *
 * @param <S> The "self" type (the actual exposed type of this collector).
 */
public interface SimpleBooleanOperatorPredicateClausesCollector<S extends SimpleBooleanOperatorPredicateClausesCollector<?>> {
	/**
	 * Adds the specified predicate to the list of clauses.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S add(PredicateFinalStep searchPredicate);

	/**
	 * Adds the specified previously-built {@link SearchPredicate} to the list of clauses.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S add(SearchPredicate searchPredicate);

	/**
	 * Adds a clause to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S add(Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

	/**
	 * Delegates setting clauses and options to a given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param contributor A consumer that will add clauses and options to the collector that it consumes.
	 * Should generally be a lambda expression.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S with(Consumer<? super S> contributor);
}
