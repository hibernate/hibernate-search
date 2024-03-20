/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a query string predicate definition, where the query string to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface CommonQueryStringPredicateMatchingStep<N extends CommonQueryStringPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given query string.
	 *
	 * @param queryString The query string to match.
	 * @return The next step.
	 */
	N matching(String queryString);

	/**
	 * Require at least one of the targeted fields to match the query string that will be passed to a query via a query parameter.
	 *
	 * @param parameterName The name of a query parameter representing the query string to match.
	 * @return The next step.
	 */
	@Incubating
	N matchingParam(String parameterName);
}
