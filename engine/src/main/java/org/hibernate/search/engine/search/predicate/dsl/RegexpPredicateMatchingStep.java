/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The step in a "regexp" predicate definition where the pattern to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface RegexpPredicateMatchingStep<N extends RegexpPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given regular expression pattern.
	 *
	 * @param regexpPattern The pattern to match.
	 * @return The next step.
	 */
	N matching(String regexpPattern);

	/**
	 * Require at least one of the targeted fields to match the pattern that will be passed to a query via a query parameter.
	 * <p>
	 * Same as {@link #matching(String)} but delaying the match value to be passed via a query parameter.
	 *
	 * @param parameterName The name of a query parameter representing the pattern to match.
	 * @return The next step.
	 */
	@Incubating
	N matchingParam(String parameterName);

}
