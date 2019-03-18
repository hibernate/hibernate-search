/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.function.Function;

import org.hibernate.search.engine.search.SearchPredicate;

/**
 * The context used when starting to define a match all predicate.
 */
public interface MatchAllPredicateContext extends SearchPredicateBoostContext<MatchAllPredicateContext>,
		SearchPredicateTerminalContext {

	/**
	 * Add a "must not" clause based on a previously-built {@link SearchPredicate},
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param searchPredicate The predicate that must not match.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateContext except(SearchPredicate searchPredicate);

	/*
	 * Syntactic sugar allowing to skip the toPredicate() call by passing a SearchPredicateTerminalContext
	 * directly.
	 */

	/**
	 * Add a "must not" clause based on an almost-built {@link SearchPredicate}.
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param terminalContext The terminal context allowing to retrieve a {@link SearchPredicate}.
	 * @return A context allowing to get the resulting predicate.
	 */
	default MatchAllPredicateContext except(SearchPredicateTerminalContext terminalContext) {
		return except( terminalContext.toPredicate() );
	}
	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing the structure of the predicate building code to mirror the structure of predicates,
	 * even for complex predicate building requiring for example if/else statements.
	 */

	/**
	 * Add a "must not" clause to be defined by the given function.
	 * <p>
	 * Best used with lambda expressions.
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param clauseContributor A function that will use the DSL context passed in parameter to create a predicate,
	 * returning the resulting terminal context.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateContext except(
			Function<? super SearchPredicateFactoryContext, ? extends SearchPredicateTerminalContext> clauseContributor);

}
