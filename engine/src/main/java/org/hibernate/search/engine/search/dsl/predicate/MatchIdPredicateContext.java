/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

import java.util.Collection;

/**
 * The context used when defining a match on an identifier.
 */
public interface MatchIdPredicateContext {

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * <p>
	 * @see #matchingAny(Collection)
	 * @param value the value of the id we want to match.
	 * @return {@code this} for method chaining.
	 */
	MatchIdPredicateTerminalContext matching(Object value);

	/**
	 * Target the identifiers matching any of the values in a collection.
	 * <p>
	 * @param values the collection of identifiers to match.
	 * @return {@code this} for method chaining.
	 */
	default MatchIdPredicateTerminalContext matchingAny(Collection<?> values) {
		MatchIdPredicateTerminalContext context = null;
		for ( Object value : values ) {
			context = matching( value );
		}
		return context;
	}
}
