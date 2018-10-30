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
public interface MatchAllPredicateContext extends SearchPredicateNoFieldContext<MatchAllPredicateContext>,
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
	 * @param clauseContributor A function that will use the context passed in parameter to create a {@link SearchPredicate}.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateContext except(Function<? super SearchPredicateContainerContext, SearchPredicate> clauseContributor);

}
