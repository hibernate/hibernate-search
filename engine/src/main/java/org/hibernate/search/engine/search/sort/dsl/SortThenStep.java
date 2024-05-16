/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;

/**
 * The step in a sort definition where another sort can be chained.
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 */
public interface SortThenStep<E> extends SortFinalStep {

	/**
	 * Start defining another sort, to be applied after the current one.
	 *
	 * @return The next step.
	 */
	SearchSortFactory<E> then();

}
