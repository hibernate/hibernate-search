/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a "minimum should match" constraint,
 * after a {@link MinimumShouldMatchContext condition was defined}.
 * <p>
 * See <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraints</a>.
 *
 * @param <N> The type of the next context (returned by {@link MinimumShouldMatchNonEmptyContext#end()}).
 */
public interface MinimumShouldMatchConditionContext<N> {

	/**
	 * Add a <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 * If positive, it is the number of clauses that have to match.
	 * See <a href="MinimumShouldMatchContext.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return A context allowing to add additional constraints.
	 */
	MinimumShouldMatchNonEmptyContext<N> thenRequireNumber(int matchingClausesNumber);

	/**
	 * Add a <a href="MinimumShouldMatchContext.html#minimumshouldmatch">"minimumShouldMatch" constraint</a>.
	 *
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 * If positive, it is the percentage of the total number of "should" clauses that have to match.
	 * See <a href="MinimumShouldMatchContext.html#minimumshouldmatch-minimum">Definition of the minimum</a>
	 * for details and possible values, in particular negative values.
	 * @return A context allowing to add additional constraints.
	 */
	MinimumShouldMatchNonEmptyContext<N> thenRequirePercent(int matchingClausesPercent);

}
