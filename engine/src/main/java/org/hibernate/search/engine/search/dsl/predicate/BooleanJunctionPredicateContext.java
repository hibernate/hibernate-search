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
 * The context used when defining a boolean junction, allowing in particular to add clauses.
 * <p>
 * Different types of clauses have different effects, see below.
 *
 * <h3 id="must">"must" clauses</h3>
 * <p>
 * "must" clauses are required to match: if they don't match, then the boolean predicate will not match.
 * <p>
 * Matching "must" clauses are taken into account in score computation.
 *
 * <h3 id="mustnot">"must not" clauses</h3>
 * <p>
 * "must not" clauses are required to not match: if they don't match, then the boolean predicate will not match.
 * <p>
 * "must not" clauses are ignored from score computation.
 * <p>
 * "must not" clauses are
 *
 * <h3 id="filter">"filter" clauses</h3>
 * <p>
 * "filter" clauses are required to match: if they don't match, then the boolean predicate will not match.
 * <p>
 * "filter" clauses are ignored from score computation,
 * and so are any clauses of boolean predicates contained in the filter clause (even "match" or "should" clauses).
 *
 * <h3 id="should">"should" clauses</h3>
 * <p>
 * "should" clauses may optionally match, and are required to match depending on the context.
 * <ul>
 * <li>
 *     When there isn't any "must" clause nor any "filter" clause in the boolean predicate,
 *     and there is no <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>,
 *     then at least one "should" clause is required to match.
 *     Simply put, in this case, the "should" clauses
 *     <strong>behave as if there was an "OR" operator between each of them</strong>.
 * </li>
 * <li>
 *     When there is at least one "must" clause or one "filter" clause in the boolean predicate,
 *     and there is no <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>,
 *     then the "should" clauses are not required to match,
 *     and are simply used for scoring.
 * </li>
 * <li>
 *     When there is at least one <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>,
 *     then the "should" clauses are required according to the "minimumShouldMatch" constraints.
 * </li>
 * </ul>
 * <p>
 * Matching "should" clauses are taken into account in score computation.
 *
 * @param <N> The type of the next context (returned by {@link SearchPredicateTerminalContext#end()}).
 */
public interface BooleanJunctionPredicateContext<N> extends
		SearchPredicateNoFieldContext<BooleanJunctionPredicateContext<N>>, SearchPredicateTerminalContext<N> {

	/**
	 * Add a <a href="#must">"must" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> must(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#mustnot">"must not" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must not match.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> mustNot(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#should">"should" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that should match.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> should(SearchPredicate searchPredicate);

	/**
	 * Add a <a href="#filter">"filter" clause</a> based on a previously-built {@link SearchPredicate}.
	 *
	 * @param searchPredicate The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> filter(SearchPredicate searchPredicate);

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing the structure of the predicate building code to mirror the structure of predicates,
	 * even for complex predicate building requiring for example if/else statements.
	 */

	/**
	 * Add a <a href="#must">"must" clause</a>,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> must(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/**
	 * Add a <a href="#mustnot">"must not" clause</a>,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> mustNot(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/**
	 * Add a <a href="#should">"should" clause</a>,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> should(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/**
	 * Add a <a href="#filter">"filter" clause</a>,
	 * which will be defined by the given consumer.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> filter(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/*
	 * Options
	 */

	/**
	 * Add a default <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="MinimumShouldMatchContext.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return {@code this}, for method chaining.
	 */
	default BooleanJunctionPredicateContext<N> minimumShouldMatchNumber(int matchingClausesNumber) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequireNumber( matchingClausesNumber )
				.end();
	}

	/**
	 * Add a default <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * See <a href="MinimumShouldMatchContext.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return {@code this}, for method chaining.
	 */
	default BooleanJunctionPredicateContext<N> minimumShouldMatchPercent(int matchingClausesPercent) {
		return minimumShouldMatch()
				.ifMoreThan( 0 ).thenRequirePercent( matchingClausesPercent )
				.end();
	}

	/**
	 * Start defining the minimum number of "should" constraints that have to match
	 * in order for the boolean predicate to match.
	 * <p>
	 * See {@link MinimumShouldMatchContext}.
	 *
	 * @return A {@link MinimumShouldMatchContext} allowing to define constraints.
	 */
	MinimumShouldMatchContext<? extends BooleanJunctionPredicateContext<N>> minimumShouldMatch();

	/**
	 * Start defining the minimum number of "should" constraints that have to match
	 * in order for the boolean predicate to match.
	 * <p>
	 * See {@link MinimumShouldMatchContext}.
	 *
	 * @param constraintContributor A consumer that will add constraints to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> minimumShouldMatch(
			Consumer<? super MinimumShouldMatchContext<?>> constraintContributor);

}
