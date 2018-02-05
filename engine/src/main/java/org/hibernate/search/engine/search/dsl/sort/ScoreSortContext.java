/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.sort;

/**
 * The context used when defining a score sort.
 *
 * @param <N> The type of the end context (returned by {@link FieldSortContext#end()}).
 *
 * @author Emmanuel Bernard emmanuel@hibernate.org
 * @author Yoann Rodiere
 */
public interface ScoreSortContext<N>
		extends NonEmptySortContext<N>, SortOrderContext<ScoreSortContext<N>> {
}
