/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;

public interface BooleanPredicateBuilder extends MinimumShouldMatchBuilder, SearchPredicateBuilder {

	void must(SearchPredicate clause);

	void should(SearchPredicate clause);

	void mustNot(SearchPredicate clause);

	void filter(SearchPredicate clause);

	/**
	 * See {@link BooleanPredicateClausesStep#minimumShouldMatch()}.
	 *
	 * @param ignoreConstraintCeiling The maximum number of "should" clauses above which this constraint
	 * will cease to be ignored.
	 * @param matchingClausesNumber A definition of the number of "should" clauses that have to match.
	 */
	void minimumShouldMatchNumber(int ignoreConstraintCeiling, int matchingClausesNumber);

	/**
	 * See {@link BooleanPredicateClausesStep#minimumShouldMatch()}.
	 *
	 * @param ignoreConstraintCeiling The maximum number of "should" clauses above which this constraint
	 * will cease to be ignored.
	 * @param matchingClausesPercent A definition of the number of "should" clauses that have to match, as a percentage.
	 */
	void minimumShouldMatchPercent(int ignoreConstraintCeiling, int matchingClausesPercent);

	boolean hasClause();

}
