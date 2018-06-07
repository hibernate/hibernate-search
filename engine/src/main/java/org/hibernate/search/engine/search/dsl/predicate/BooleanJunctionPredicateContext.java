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
 *     then at least one "should" clause is required to match.
 *     Simply put, in this case, the "should" clauses
 *     <strong>behave as if there was an "OR" operator between each of them</strong>.
 * </li>
 * <li>
 *     When there is at least one "must" clause or one "filter" clause in the boolean predicate,
 *     then the "should" clauses are not required to match,
 *     and are simply used for scoring.
 * </li>
 * </ul>
 * <p>
 * Matching "should" clauses are taken into account in score computation.
 *
 * @param <N> The type of the next context (returned by {@link ExplicitEndContext#end()}).
 */
public interface BooleanJunctionPredicateContext<N> extends SearchPredicateContext<BooleanJunctionPredicateContext<N>>, ExplicitEndContext<N> {

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
	 * Fully fluid syntax.
	 */

	/**
	 * Create a context allowing to define a <a href="#must">"must" clause</a>.
	 *
	 * @return A {@link SearchPredicateContainerContext} allowing to define the predicate of the "must" clause.
	 */
	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> must();

	/**
	 * Create a context allowing to define a <a href="#mustnot">"must not" clause</a>.
	 *
	 * @return A {@link SearchPredicateContainerContext} allowing to define the predicate of the "must not" clause.
	 */
	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> mustNot();

	/**
	 * Create a context allowing to define a <a href="#should">"should" clause</a>.
	 *
	 * @return A {@link SearchPredicateContainerContext} allowing to define the predicate of the "should" clause.
	 */
	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> should();

	/**
	 * Create a context allowing to define a <a href="#filter">"filter" clause</a>.
	 *
	 * @return A {@link SearchPredicateContainerContext} allowing to define the predicate of the "filter" clause.
	 */
	SearchPredicateContainerContext<? extends BooleanJunctionPredicateContext<N>> filter();

	/*
	 * Alternative syntax taking advantage of lambdas,
	 * allowing to introduce if/else statements in the query building code.
	 */

	/**
	 * Create a context allowing to define a <a href="#must">"must" clause</a>,
	 * and apply a consumer to it.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> must(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/**
	 * Create a context allowing to define a <a href="#mustnot">"must not" clause</a>,
	 * and apply a consumer to it.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> mustNot(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/**
	 * Create a context allowing to define a <a href="#should">"should" clause</a>,
	 * and apply a consumer to it.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> should(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

	/**
	 * Create a context allowing to define a <a href="#filter">"filter" clause</a>,
	 * and apply a consumer to it.
	 * <p>
	 * Best used with lambda expressions.
	 *
	 * @param clauseContributor A consumer that will add clauses to the context passed in parameter.
	 * Should generally be a lambda expression.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> filter(Consumer<? super SearchPredicateContainerContext<?>> clauseContributor);

}
