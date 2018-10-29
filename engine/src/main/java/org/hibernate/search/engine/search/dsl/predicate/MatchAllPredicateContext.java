/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Consumer;

import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.dsl.ExplicitEndContext;

/**
 * The context used when starting to define a match all predicate.
 *
 * @param <N> The type of the next context (returned by {@link ExplicitEndContext#end()}).
 */
public interface MatchAllPredicateContext<N> extends SearchPredicateNoFieldContext<MatchAllPredicateContext<N>>, ExplicitEndContext<N> {

	/**
	 * Add a "must not" clause based on a previously-built {@link SearchPredicate},
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param searchPredicate The predicate that must not match.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateContext<N> except(SearchPredicate searchPredicate);

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing the structure of the predicate building code to mirror the structure of predicates,
	 * even for complex predicate building requiring for example if/else statements.
	 */

	/**
	 * Create a context allowing to define a "must not" clause,
	 * and apply a consumer to it.
	 * <p>
	 * Best used with lambda expressions.
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateContext<N> except(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

}
