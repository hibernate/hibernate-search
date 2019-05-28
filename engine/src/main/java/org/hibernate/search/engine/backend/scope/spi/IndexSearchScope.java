/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.scope.spi;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;

/**
 * The scope of a search, aware of the targeted indexes and of the underlying technology (backend).
 *
 * @param <C> The type of query element collector
 */
public interface IndexSearchScope<C> {

	SearchPredicateBuilderFactory<? super C, ?> getSearchPredicateBuilderFactory();

	SearchSortBuilderFactory<? super C, ?> getSearchSortBuilderFactory();

	SearchQueryBuilderFactory<C> getSearchQueryBuilderFactory();

	SearchProjectionBuilderFactory getSearchProjectionFactory();

}
