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
 *     and there is no "minimumShouldMatch" constraint (see below),
 *     then at least one "should" clause is required to match.
 *     Simply put, in this case, the "should" clauses
 *     <strong>behave as if there was an "OR" operator between each of them</strong>.
 * </li>
 * <li>
 *     When there is at least one "must" clause or one "filter" clause in the boolean predicate,
 *     and there is no "minimumShouldMatch" constraint (see below),
 *     then the "should" clauses are not required to match,
 *     and are simply used for scoring.
 * </li>
 * <li>
 *     When there is at least one "minimumShouldMatch" constraint (see below),
 *     then the "should" clauses are required according to the "minimumShouldMatch" constraints.
 * </li>
 * </ul>
 * <p>
 * Matching "should" clauses are taken into account in score computation.
 *
 * <h3 id="minimumshouldmatch">"minimumShouldMatch" constraints</h3>
 * <p>
 * "minimumShouldMatch" constraints define a minimum number of "should" clauses that have to match
 * in order for the boolean predicate to match.
 * <p>
 * The feature is similar, and will work identically, to
 * <a href="https://lucene.apache.org/solr/7_3_0/solr-core/org/apache/solr/util/doc-files/min-should-match.html">"Min Number Should Match"</a>
 * in Solr or
 * <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-minimum-should-match.html">{@code minimum_should_match}</a>
 * in Elasticsearch.
 *
 * <h4 id="minimumshouldmatch-minimum">Definition of the minimum</h4>
 * <p>
 * The minimum may be defined either directly as a positive number, or indirectly as a negative number
 * or positive or negative percentage representing a ratio of the total number of "should" clauses in this boolean predicate.
 * <p>
 * Here is how each type of input is interpreted:
 * <dl>
 *     <dt>Positive number</dt>
 *     <dd>
 *         The value is interpreted directly as the minimum number of "should" clauses that have to match.
 *     </dd>
 *     <dt>Negative number</dt>
 *     <dd>
 *         The absolute value is interpreted as the maximum number of "should" clauses that may not match:
 *         the absolute value is subtracted from the total number of "should" clauses.
 *     </dd>
 *     <dt>Positive percentage</dt>
 *     <dd>
 *         The value is interpreted as the minimum percentage of the total number of "should" clauses that have to match:
 *         the percentage is applied to the total number of "should" clauses, then rounded down.
 *     </dd>
 *     <dt>Negative percentage</dt>
 *     <dd>
 *         The absolute value is interpreted as the maximum percentage of the total number of "should" clauses that may not match:
 *         the absolute value of the percentage is applied to the total number of "should" clauses, then rounded down,
 *         then subtracted from the total number of "should" clauses.
 *     </dd>
 * </dl>
 * <p>
 * In any case, if the computed minimum is 0 or less, or higher than the total number of "should" clauses,
 * behavior is backend-specific (it may throw an exception, or produce unpredictable results,
 * or fall back to some default behavior).
 *
 * <h4 id="minimumshouldmatch-conditionalconstraints">Conditional constraints</h4>
 * <p>
 * Multiple conditional constraints may be defined,
 * only one of them being applied depending on the total number of "should" clauses.
 * <p>
 * Each constraint is attributed a minimum number of "should" clauses
 * that have to match, <strong>and an additional number</strong>.
 * The additional number is unique to each constraint.
 * <p>
 * The additional number will be compared to the total number of "should" clauses,
 * and the one closest while still strictly lower will be picked: its associated constraint will be applied.
 * If no number matches, the minimum number of matching "should" clauses
 * will be set to the total number of "should" clauses.
 * <p>
 * Examples:
 * <pre><code>
 *     // Example 1: at least 3 "should" clauses have to match
 *     booleanContext1.minimumShouldMatchNumber( 3 );
 *     // Example 2: at most 2 "should" clauses may not match
 *     booleanContext2.minimumShouldMatchNumber( -2 );
 *     // Example 3: at least 75% of "should" clauses have to match (rounded down)
 *     booleanContext3.minimumShouldMatchPercent( 75 );
 *     // Example 4: at most 25% of "should" clauses may not match (rounded down)
 *     booleanContext4.minimumShouldMatchPercent( -25 );
 *     // Example 5: if there are 3 "should" clauses or less, all "should" clauses have to match.
 *     // If there are 4 "should" clauses or more, at least 90% of "should" clauses have to match (rounded down).
 *     booleanContext5.minimumShouldMatchPercent( 3, 90 );
 *     // Example 6: if there are 4 "should" clauses or less, all "should" clauses have to match.
 *     // If there are 5 to 9 "should" clauses, at most 25% of "should" clauses may not match (rounded down).
 *     // If there are 10 "should" clauses or more, at most 3 "should" clauses may not match.
 *     booleanContext6.minimumShouldMatchPercent( 4, 25 )
 *             .minimumShouldMatchNumber( 9, -3 );
 * </code></pre>
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

	/*
	 * Options
	 */

	/**
	 * Add a default <a href="#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="#minimumshouldmatch-minimum">Definition of the minimum</a> for details and possible values.
	 * @return {@code this}, for method chaining.
	 */
	default BooleanJunctionPredicateContext<N> minimumShouldMatchNumber(int matchingClausesNumber) {
		return minimumShouldMatchNumber( 0, matchingClausesNumber );
	}

	/**
	 * Add a default <a href="#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * See <a href="#minimumshouldmatch-minimum">Definition of the minimum</a> for details and possible values.
	 * @return {@code this}, for method chaining.
	 */
	default BooleanJunctionPredicateContext<N> minimumShouldMatchPercent(int matchingClausesPercent) {
		return minimumShouldMatchPercent( 0, matchingClausesPercent );
	}

	/**
	 * Add a <a href="#minimumshouldmatch">"minimumShouldMatch" constraint</a> that will be applied
	 * if there are strictly more than {@code ignoreConstraintCeiling} "should" clauses.
	 * <p>
	 * See <a href="#minimumshouldmatch-conditionalconstraints">Conditional constraints</a> for the detailed rules
	 * defining whether a constraint is applied or not.
	 *
	 * @param ignoreConstraintCeiling The maximum number of "should" clauses above which this constraint
	 * will cease to be ignored.
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="#minimumshouldmatch-minimum">Definition of the minimum</a> for details and possible values.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber);

	/**
	 * Add a <a href="#minimumshouldmatch">"minimumShouldMatch" constraint</a> that will be applied
	 * if there are strictly more than {@code ignoreConstraintCeiling} "should" clauses.
	 * <p>
	 * See <a href="#minimumshouldmatch-conditionalconstraints">Conditional constraints</a> for the detailed rules
	 * defining whether a constraint is applied or not.
	 * @param ignoreConstraintCeiling The maximum number of "should" clauses above which this constraint
	 * will cease to be ignored.
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * @return {@code this}, for method chaining.
	 */
	BooleanJunctionPredicateContext<N> minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent);

}
