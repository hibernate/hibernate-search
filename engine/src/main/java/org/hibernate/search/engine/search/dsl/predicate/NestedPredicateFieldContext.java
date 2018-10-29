/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;

/**
 * The context used when defining a "nested" predicate, after the object field was mentioned.
 *
 * @param <N> The type of the next context (returned after the nested query was defined).
 */
public interface NestedPredicateFieldContext<N> {

	// TODO add tuning methods, like the "score_mode" in Elasticsearch (avg, min, ...)

	/**
	 * Set the inner predicate to a previously-built {@link SearchPredicate}.
	 * <p>
	 * Matching documents are those for which at least one element of the nested object field
	 * matches the inner predicate.
	 *
	 * @param searchPredicate The predicate that must be matched by at least one element of the nested object field.
	 * @return A context allowing to end the predicate definition.
	 */
	SearchPredicateTerminalContext<N> nest(SearchPredicate searchPredicate);

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing the structure of the predicate building code to mirror the structure of predicates,
	 * even for complex predicate building requiring for example if/else statements.
	 */

	/**
	 * Create a context allowing to define the inner predicate,
	 * and apply a consumer to it.
	 * <p>
	 * Best used with lambda expressions.
	 * <p>
	 * Matching documents are those for which at least one element of the nested object field
	 * matches the inner predicate.
	 *
	 * @param predicateContributor A consumer that will add a predicate to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return A context allowing to end the predicate definition.
	 */
	SearchPredicateTerminalContext<N> nest(Consumer<? super SearchPredicateContainerContext<?>> predicateContributor);

}
