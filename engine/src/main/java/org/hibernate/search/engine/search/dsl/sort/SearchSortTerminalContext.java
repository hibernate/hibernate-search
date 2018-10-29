/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;


/**
 * The terminal context of the sort DSL.
 *
 * @param <N> The type of the next context (returned by {@link #end()}).
 */
public interface SearchSortTerminalContext<N> {

	/**
	 * End the current context and continue to the next one.
	 *
	 * @return The next context.
	 */
	N end();

}
