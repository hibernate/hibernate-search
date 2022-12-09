/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * An object where the clauses and options of a simple boolean predicate
 * ({@link SearchPredicateFactory#and() and}, {@link SearchPredicateFactory#or() or})
 * can be set.
 *
 * <h2 id="clauses">Clauses</h2>
 * <p>
 * Depending on the outer predicate,
 * documents will have to match either <em>all</em> clauses,
 * or <em>any</em> clause:
 * <ul>
 * <li>
 *     For the {@link SearchPredicateFactory#and() and} predicate,
 *     documents will have to match <em>all</em> clauses.
 * </li>
 * <li>
 *     For the {@link SearchPredicateFactory#or() or} predicate,
 *     documents will have to match <em>any</em> clauses (at least one).
 * </li>
 * <li>
 *     For the {@link SearchPredicateFactory#nested(String) nested} predicate,
 *     documents will have to match <em>all</em> clauses.
 * </li>
 * <li>
 *     For the root predicate defined through the second parameter of the lambda passed
 *     to {@link org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep#where(BiConsumer)},
 *     documents will have to match <em>all</em> clauses.
 * </li>
 * </ul>
 *
 * @param <S> The "self" type (the actual exposed type of this collector).
 */
public interface SimpleBooleanPredicateClausesCollector<S extends SimpleBooleanPredicateClausesCollector<?>> {
	/**
	 * Adds the specified predicate to the list of <a href="#clauses">clauses</a>.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S add(PredicateFinalStep searchPredicate);

	/**
	 * Adds the specified previously-built {@link SearchPredicate} to the list of <a href="#clauses">clauses</a>.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S add(SearchPredicate searchPredicate);

	/**
	 * Adds a <a href="#clauses">clause</a> to be defined by the given function.
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
	 * Delegates setting <a href="#clauses">clauses</a> and options to a given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param contributor A consumer that will add clauses and options to the collector that it consumes.
	 * Should generally be a lambda expression.
	 *
	 * @return {@code this}, for method chaining.
	 */
	S with(Consumer<? super S> contributor);

	/**
	 * Checks if this predicate contains at least one clause.
	 *
	 * @return {@code true} if any clauses were added, i.e. any of the {@code add(..)} methods
	 * were called at least once, {@code false} otherwise.
	 */
	boolean hasClause();
}
