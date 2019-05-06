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
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
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

	public SearchQueryResultContext<?, IndexSearchQuery<O>, ?> asObject() {
		return asObject( Function.identity() );
	}

	public <Q> SearchQueryResultContext<?, Q, ?> asObject(Function<IndexSearchQuery<O>, Q> searchQueryWrapperFactory) {
		return searchScopeDelegate.queryAsLoadedObject(
				sessionContext, loadingContextBuilder, searchQueryWrapperFactory
		);
	}

	public SearchQueryResultContext<?, IndexSearchQuery<R>, ?> asReference() {
		return asReference( Function.identity() );
	}

	public <Q> SearchQueryResultContext<?, Q, ?> asReference(
			Function<IndexSearchQuery<R>, Q> searchQueryWrapperFactory) {
		return searchScopeDelegate.queryAsReference(
				sessionContext, loadingContextBuilder, searchQueryWrapperFactory
		);
	}

	public <P> SearchQueryResultContext<?, IndexSearchQuery<P>, ?> asProjection(
			Function<SearchProjectionFactoryContext<R, O>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		return asProjection( projectionContributor.apply( searchScopeDelegate.projection() ).toProjection() );
	}

	public <P> SearchQueryResultContext<?, IndexSearchQuery<P>, ?> asProjection(SearchProjection<P> projection) {
		return asProjection( Function.identity(), projection );
	}

	public <T, Q> SearchQueryResultContext<?, Q, ?> asProjection(
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<T> projection) {
		return searchScopeDelegate.queryAsProjection(
				sessionContext, loadingContextBuilder, searchQueryWrapperFactory,
				projection
		);
	}

	public SearchQueryResultContext<?, IndexSearchQuery<List<?>>, ?> asProjections(SearchProjection<?>... projections) {
		return asProjections( Function.identity(), projections );
	}

	public <Q> SearchQueryResultContext<?, Q, ?> asProjections(
			Function<IndexSearchQuery<List<?>>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections) {
		return searchScopeDelegate.queryAsProjections(
				sessionContext, loadingContextBuilder, searchQueryWrapperFactory,
				projections
		);
	}
}
