/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.spi;

import org.hibernate.search.engine.search.predicate.spi.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortFactory;

/**
 * The target context during a search, aware of the targeted indexes and of the underlying technology (backend).
 *
 * @param <C> The type of query element collector
 */
public interface SearchTargetContext<C> {

	SearchPredicateFactory<? super C, ?> getSearchPredicateFactory();

	SearchSortFactory<? super C, ?> getSearchSortFactory();

	SearchQueryFactory<C> getSearchQueryFactory();

	SearchProjectionFactory getSearchProjectionFactory();

}
