/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.Collection;

/**
 * The step in a "match id" predicate definition where the IDs to match can be set.
 */
public interface MatchIdPredicateMatchingStep {

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * <p>
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @return The next step.
	 */
	MatchIdPredicateMatchingMoreStep matching(Object value);

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * <p>
	 * @param values the collection of identifiers to match.
	 * @return The next step.
	 */
	default MatchIdPredicateMatchingMoreStep matchingAny(Collection<?> values) {
		MatchIdPredicateMatchingMoreStep next = null;
		for ( Object value : values ) {
			next = matching( value );
		}
		return next;
	}
}
