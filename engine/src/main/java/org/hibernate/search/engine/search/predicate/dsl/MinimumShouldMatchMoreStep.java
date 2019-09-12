/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "minimum should match" constraint definition
 * where the constraint definition can be {@link #end() ended},
 * or {@link #ifMoreThan(int) more conditions can be added}.
 * <p>
 * See <a href="MinimumShouldMatchConditionStep.html#minimumshouldmatch">"minimumShouldMatch" constraints</a>.
 *
 * @param <N> The type of the next step of the predicate definition (returned by {@link MinimumShouldMatchMoreStep#end()}).
 */
public interface MinimumShouldMatchMoreStep<N> extends MinimumShouldMatchConditionStep<N> {

	/**
	 * End the "minimum should match" constraint definition and continue the predicate definition.
	 *
	 * @return The next step of the predicate definition.
	 */
	N end();

}
