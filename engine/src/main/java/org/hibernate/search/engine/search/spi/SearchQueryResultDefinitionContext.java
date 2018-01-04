/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.spi;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchQuery;

/**
 * The context used when building a query, when the search result type must be defined.
 */
public interface SearchQueryResultDefinitionContext<R, O> {

	SearchQueryWrappingDefinitionResultContext<SearchQuery<O>> asObjects();

	default SearchQueryWrappingDefinitionResultContext<SearchQuery<R>> asReferences() {
		return asReferences( Function.identity() );
	}

	default SearchQueryWrappingDefinitionResultContext<SearchQuery<List<?>>> asProjections(String ... projections) {
		return asProjections( Function.identity(), projections );
	}

	<T> SearchQueryWrappingDefinitionResultContext<SearchQuery<T>> asReferences(Function<R, T> hitTransformer);

	<T> SearchQueryWrappingDefinitionResultContext<SearchQuery<T>> asProjections(Function<List<?>, T> hitTransformer,
			String ... projections);

}
