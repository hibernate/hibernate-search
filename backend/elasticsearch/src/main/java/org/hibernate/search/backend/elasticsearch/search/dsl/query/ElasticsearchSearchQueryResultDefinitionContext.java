/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultDefinitionContext;

public interface ElasticsearchSearchQueryResultDefinitionContext<R, E>
		extends SearchQueryResultDefinitionContext<R, E, ElasticsearchSearchProjectionFactoryContext<R, E>> {

	@Override
	ElasticsearchSearchQueryResultContext<E> asEntity();

	@Override
	ElasticsearchSearchQueryResultContext<R> asReference();

	@Override
	<P> ElasticsearchSearchQueryResultContext<P> asProjection(
			Function<? super ElasticsearchSearchProjectionFactoryContext<R, E>, ? extends SearchProjectionTerminalContext<P>> projectionContributor);

	@Override
	<P> ElasticsearchSearchQueryResultContext<P> asProjection(SearchProjection<P> projection);

	@Override
	ElasticsearchSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections);

}
