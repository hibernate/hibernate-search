/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;

/**
 * The context used when defining a match on an identifier.
 */
public interface MatchIdPredicateContext extends SearchPredicateTerminalContext {

	/**
	 * Target the identifier with the given id.
	 * <p>
	 * If used multiple times, it will target any of the specified values.
	 * <p>
	 * @param value the value of the id we want to match.
	 * @return {@code this} for method chaining.
	 */
	MatchIdPredicateContext matching(Object value);
}
