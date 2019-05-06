/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.mapper.javabean.search.dsl.query.SearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.javabean.search.query.SearchQuery;
import org.hibernate.search.mapper.javabean.search.query.impl.JavaBeanSearchQuery;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class SearchQueryResultDefinitionContextImpl implements SearchQueryResultDefinitionContext {
	private final PojoSearchScopeDelegate<?, PojoReference> searchScopeDelegate;

	public SearchQueryResultDefinitionContextImpl(PojoSearchScopeDelegate<?, PojoReference> searchScopeDelegate) {
		this.searchScopeDelegate = searchScopeDelegate;
	}

	@Override
	public SearchQueryResultContext<?, SearchQuery<PojoReference>, ?> asReference() {
		return searchScopeDelegate.queryAsReference( JavaBeanSearchQuery::new );
	}

	@Override
	public <T> SearchQueryResultContext<?, SearchQuery<T>, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, ?>, ? extends SearchProjectionTerminalContext<T>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchScopeDelegate.projection() ).toProjection() );
	}

	@Override
	public <T> SearchQueryResultContext<?, SearchQuery<T>, ?> asProjection(SearchProjection<T> projection) {
		return searchScopeDelegate.queryAsProjection(
				ObjectLoader.identity(), JavaBeanSearchQuery::new, projection
		);
	}

	@Override
	public SearchQueryResultContext<?, SearchQuery<List<?>>, ?> asProjections(SearchProjection<?>... projections) {
		return searchScopeDelegate.queryAsProjections(
				ObjectLoader.identity(), JavaBeanSearchQuery::new, projections
		);
	}
}
