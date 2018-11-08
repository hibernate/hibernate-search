/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.dsl.query;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;

public interface JavaBeanQueryResultDefinitionContext {

	SearchQueryResultContext<SearchQuery<PojoReference>> asReferences();

	<T> SearchQueryResultContext<SearchQuery<T>> asReferences(Function<PojoReference, T> hitTransformer);

	<P> SearchQueryResultContext<SearchQuery<P>> asProjections(SearchProjection<P> projection);

	<P, T> SearchQueryResultContext<SearchQuery<T>> asProjections(Function<P, T> hitTransformer,
			SearchProjection<P> projection);

	SearchQueryResultContext<SearchQuery<List<?>>> asProjections(SearchProjection<?>... projections);

	<T> SearchQueryResultContext<SearchQuery<T>> asProjections(Function<List<?>, T> hitTransformer,
			SearchProjection<?>... projections);
}
