/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in an "simple query string" predicate definition where the query string to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface QueryStringPredicateMatchingStep<N extends QueryStringPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given query string.
	 *
	 * @param simpleQueryString The query string to match.
	 * @return The next step.
	 */
	N matching(String simpleQueryString);

}
