/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

/**
 * A superinterface for contexts allowing to define sorts.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface NonEmptySortContext extends SearchSortTerminalContext {

	/**
	 * Start defining another sort, to be applied after the current one.
	 *
	 * @return A {@link SearchSortFactoryContext} allowing to define a sort.
	 */
	SearchSortFactoryContext then();

}
