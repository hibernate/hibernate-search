/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "match" predicate definition where the value to match can be set.
 *
 * @param <N> The type of the next step.
 * @param <T> The type of the match value.
 */
public interface MatchPredicateMatchingGenericStep<N extends MatchPredicateOptionsStep<?>, T> {

	/**
	 * Require at least one of the targeted fields to match the given value.
	 *
	 * @param value The value to match.
	 * The signature of this method defines this parameter as an {@code T},
	 * but a specific type is expected depending on the targeted field.
	 * @return The next step.
	 */
	N matching(T value);
}
