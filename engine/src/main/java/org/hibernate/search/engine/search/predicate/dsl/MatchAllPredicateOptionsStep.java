/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.function.Function;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * The initial and final step in "match all" predicate definition, where optional parameters can be set.
 */
public interface MatchAllPredicateOptionsStep
		extends PredicateFinalStep, PredicateBoostStep<MatchAllPredicateOptionsStep> {

	/**
	 * Add a "must not" clause based on a previously-built {@link SearchPredicate},
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param searchPredicate The predicate that must not match.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateOptionsStep except(SearchPredicate searchPredicate);

	/*
	 * Syntactic sugar allowing to skip the toPredicate() call by passing a PredicateFinalStep
	 * directly.
	 */

	/**
	 * Add a "must not" clause based on an almost-built {@link SearchPredicate}.
	 * <p>
	 * Documents matching the "must not" clause won't match the "match all" predicate.
	 *
	 * @param dslFinalStep A final step in the predicate DSL allowing the retrieval of a {@link SearchPredicate}.
	 * @return {@code this}, for method chaining.
	 */
	default MatchAllPredicateOptionsStep except(PredicateFinalStep dslFinalStep) {
		return except( dslFinalStep.toPredicate() );
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
	 * @param clauseContributor A function that will use the factory passed in parameter to create a predicate,
	 * returning the final step in the predicate DSL.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	MatchAllPredicateOptionsStep except(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> clauseContributor);

}
