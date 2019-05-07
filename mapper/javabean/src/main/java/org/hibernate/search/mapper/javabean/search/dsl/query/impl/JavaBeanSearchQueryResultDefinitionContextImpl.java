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
import org.hibernate.search.mapper.javabean.search.dsl.query.JavaBeanSearchQueryResultDefinitionContext;
import org.hibernate.search.mapper.javabean.search.loading.context.impl.JavaBeanLoadingContext;
import org.hibernate.search.mapper.pojo.search.PojoReference;
import org.hibernate.search.mapper.pojo.search.spi.PojoSearchScopeDelegate;

public class JavaBeanSearchQueryResultDefinitionContextImpl implements JavaBeanSearchQueryResultDefinitionContext<Void> {
	private final PojoSearchScopeDelegate<?, Void> searchScopeDelegate;
	private final JavaBeanLoadingContext.Builder loadingContextBuilder;

	public JavaBeanSearchQueryResultDefinitionContextImpl(PojoSearchScopeDelegate<?, Void> searchScopeDelegate) {
		this.searchScopeDelegate = searchScopeDelegate;
		this.loadingContextBuilder = new JavaBeanLoadingContext.Builder( searchScopeDelegate );
	}

	@Override
	public SearchQueryResultContext<?, Void, ?> asEntity() {
		return searchScopeDelegate.queryAsLoadedObject( loadingContextBuilder );
	}

	@Override
	public SearchQueryResultContext<?, PojoReference, ?> asReference() {
		return searchScopeDelegate.queryAsReference( loadingContextBuilder );
	}

	@Override
	public <T> SearchQueryResultContext<?, T, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<PojoReference, Void>, ? extends SearchProjectionTerminalContext<T>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchScopeDelegate.projection() ).toProjection() );
	}

	@Override
	public <T> SearchQueryResultContext<?, T, ?> asProjection(SearchProjection<T> projection) {
		return searchScopeDelegate.queryAsProjection(
				loadingContextBuilder, projection
		);
	}

	@Override
	public SearchQueryResultContext<?, List<?>, ?> asProjections(SearchProjection<?>... projections) {
		return searchScopeDelegate.queryAsProjections(
				loadingContextBuilder, projections
		);
	}
}
