/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

import org.hibernate.search.engine.search.SearchSort;

/**
 * The context used when defining a composite sort explicitly
 * (not using {@link NonEmptySortContext#then()}).
 */
public interface CompositeSortContext extends NonEmptySortContext {

	/**
	 * Add an element to the composite sort based on a previously-built {@link SearchSort}.
	 *
	 * @param searchSort The predicate that must match.
	 * @return {@code this}, for method chaining.
	 */
	CompositeSortContext add(SearchSort searchSort);

	/**
	 * Add an element to the composite sort based on an almost-built {@link SearchSort}.
	 *
	 * @param terminalContext The terminal context allowing to retrieve a {@link SearchSort}.
	 * @return {@code this}, for method chaining.
	 */
	default CompositeSortContext add(SearchSortTerminalContext terminalContext) {
		return add( terminalContext.toSort() );
	}

}
