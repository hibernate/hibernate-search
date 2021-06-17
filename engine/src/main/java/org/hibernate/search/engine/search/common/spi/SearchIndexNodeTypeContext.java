/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.common.spi;

/**
 * Information about the type of an composite index element targeted by search,
 * be it in a projection, a predicate, a sort, ...
 *
 * @param <SC> The type of the backend-specific search scope.
 * @param <N> The type representing the targeted index node.
 */
public interface SearchIndexNodeTypeContext<SC extends SearchIndexScope<?>, N> {

	<T> SearchQueryElementFactory<? extends T, ? super SC, ? super N>
			queryElementFactory(SearchQueryElementTypeKey<T> key);

}
