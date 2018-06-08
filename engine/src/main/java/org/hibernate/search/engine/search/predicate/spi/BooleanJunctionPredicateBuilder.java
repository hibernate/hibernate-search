/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.spi;

public interface BooleanJunctionPredicateBuilder<C> extends SearchPredicateBuilder<C> {

	C getMustCollector();

	C getMustNotCollector();

	C getShouldCollector();

	C getFilterCollector();

	/**
	 * See {@link org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext#minimumShouldMatchNumber(int, int)}.
	 *
	 * @param ignoreConstraintCeiling The maximum number of "should" clauses above which this constraint
	 * will cease to be ignored.
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 */
	void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber);

	/**
	 * See {@link org.hibernate.search.engine.search.dsl.predicate.BooleanJunctionPredicateContext#minimumShouldMatchPercent(int, int)}.
	 *
	 * @param ignoreConstraintCeiling The maximum number of "should" clauses above which this constraint
	 * will cease to be ignored.
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 */
	void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent);

}
