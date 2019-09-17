/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.predicate.dsl;

import java.util.Collection;

/**
 * The step in a "match id" predicate definition where the IDs to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface MatchIdPredicateMatchingStep<N extends MatchIdPredicateMatchingMoreStep<? extends N, ?>> {

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * <p>
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @return The next step.
	 */
	N matching(Object value);

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * <p>
	 * @param values the collection of identifiers to match.
	 * @return The next step.
	 */
	default N matchingAny(Collection<?> values) {
		N next = null;
		for ( Object value : values ) {
			next = matching( value );
		}
		return next;
	}
}
