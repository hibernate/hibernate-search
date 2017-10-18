/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.spi.SearchWrappingDefinitionContext;

/**
 * @author Yoann Rodiere
 */
public interface SearchResultDefinitionContext<R> {

	default SearchWrappingDefinitionContext<SearchQuery<R>> asReferences() {
		return asReferences( Function.identity() );
	}

	default SearchWrappingDefinitionContext<SearchQuery<List<?>>> asProjections(String ... projections) {
		return asProjections( Function.identity(), projections );
	}

	<T> SearchWrappingDefinitionContext<SearchQuery<T>> asReferences(Function<R, T> hitTransformer);

	<T> SearchWrappingDefinitionContext<SearchQuery<T>> asProjections(Function<List<?>, T> hitTransformer,
			String ... projections);

}
