/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.scope.spi;

import org.hibernate.search.engine.backend.scope.IndexScopeExtension;
import org.hibernate.search.engine.search.aggregation.spi.SearchAggregationBuilderFactory;
import org.hibernate.search.engine.search.predicate.spi.SearchPredicateBuilderFactory;
import org.hibernate.search.engine.search.projection.spi.SearchProjectionBuilderFactory;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;
import org.hibernate.search.engine.search.sort.spi.SearchSortBuilderFactory;
import org.hibernate.search.util.common.SearchException;

/**
 * The scope of an index-related operation, aware of the targeted indexes and of the underlying technology (backend).
 *
 * @param <C> The type of query element collector
 */
public interface IndexScope<C> {

	SearchPredicateBuilderFactory<? super C> searchPredicateBuilderFactory();

	SearchSortBuilderFactory<? super C> searchSortBuilderFactory();

	SearchQueryBuilderFactory<C> searchQueryBuilderFactory();

	SearchProjectionBuilderFactory searchProjectionFactory();

	SearchAggregationBuilderFactory<? super C> searchAggregationFactory();

	/**
	 * Extend the current index scope with the given extension,
	 * resulting in an extended index scope offering backend-specific utilities.
	 *
	 * @param extension The extension to apply.
	 * @param <T> The type of index scope provided by the extension.
	 * @return The extended index scope.
	 * @throws SearchException If the extension cannot be applied (wrong underlying technology, ...).
	 */
	default <T> T extension(IndexScopeExtension<T> extension) {
		return extension.extendOrFail( this );
	}

}
