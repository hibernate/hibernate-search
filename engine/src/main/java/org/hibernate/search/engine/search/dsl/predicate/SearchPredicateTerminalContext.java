/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.predicate;


import org.hibernate.search.engine.search.SearchPredicate;

/**
 * The terminal context of the predicate DSL.
 */
public interface SearchPredicateTerminalContext {

	/**
	 * Create a {@link SearchPredicate} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchPredicate} resulting from the previous DSL steps.
	 */
	SearchPredicate toPredicate();

}
