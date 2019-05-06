/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

public final class StubMappingQueryResultDefinitionContext<R, O> {

	private final MappedIndexSearchScope<R, O> searchScopeDelegate;
	private final StubSessionContext sessionContext;
	private final LoadingContextBuilder<R, O> loadingContextBuilder;

	StubMappingQueryResultDefinitionContext(MappedIndexSearchScope<R, O> searchScopeDelegate,
			StubSessionContext sessionContext,
			LoadingContextBuilder<R, O> loadingContextBuilder) {
		this.searchScopeDelegate = searchScopeDelegate;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	public SearchQueryResultContext<?, SearchQuery<O>, ?> asObject() {
		return searchScopeDelegate.queryAsLoadedObject(
				sessionContext, loadingContextBuilder
		);
	}

	public SearchQueryResultContext<?, SearchQuery<R>, ?> asReference() {
		return searchScopeDelegate.queryAsReference(
				sessionContext, loadingContextBuilder
		);
	}

	public <P> SearchQueryResultContext<?, SearchQuery<P>, ?> asProjection(
			Function<SearchProjectionFactoryContext<R, O>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchScopeDelegate.projection() ).toProjection() );
	}

	public <P> SearchQueryResultContext<?, SearchQuery<P>, ?> asProjection(
			SearchProjection<P> projection) {
		return searchScopeDelegate.queryAsProjection(
				sessionContext, loadingContextBuilder,
				projection
		);
	}

	public SearchQueryResultContext<?, SearchQuery<List<?>>, ?> asProjections(
			SearchProjection<?>... projections) {
		return searchScopeDelegate.queryAsProjections(
				sessionContext, loadingContextBuilder,
				projections
		);
	}
}
