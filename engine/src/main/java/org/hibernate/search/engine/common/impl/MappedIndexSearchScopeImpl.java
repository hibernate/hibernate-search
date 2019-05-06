/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.query.impl.DefaultSearchQueryContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.DefaultSearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.DefaultSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

class MappedIndexSearchScopeImpl<C, R, O> implements MappedIndexSearchScope<R, O> {

	private final IndexSearchScope<C> delegate;

	MappedIndexSearchScopeImpl(IndexSearchScope<C> delegate) {
		this.delegate = delegate;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "delegate=" ).append( delegate )
				.append( "]" )
				.toString();
	}

	@Override
	public <T, Q> SearchQueryResultContext<?, Q, ?> queryAsLoadedObject(SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, T> loadingContextBuilder,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		SearchQueryBuilder<T, C> builder = delegate.getSearchQueryBuilderFactory()
				.asObject( sessionContext, loadingContextBuilder );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <Q> SearchQueryResultContext<?, Q, ?> queryAsReference(SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, ?> loadingContextBuilder,
			Function<IndexSearchQuery<R>, Q> searchQueryWrapperFactory) {
		SearchQueryBuilder<R, C> builder = delegate.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <T, Q> SearchQueryResultContext<?, Q, ?> queryAsProjection(SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, O> loadingContextBuilder,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory, SearchProjection<T> projection) {
		SearchQueryBuilder<T, C> builder = delegate.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <Q> SearchQueryResultContext<?, Q, ?> queryAsProjections(
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, O> loadingContextBuilder,
			Function<IndexSearchQuery<List<?>>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections) {
		SearchQueryBuilder<List<?>, C> builder = delegate.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return new DefaultSearchPredicateFactoryContext<>( delegate.getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortContainerContext sort() {
		return new DefaultSearchSortContainerContext<>( delegate.getSearchSortBuilderFactory() );
	}

	@Override
	public SearchProjectionFactoryContext<R, O> projection() {
		return new DefaultSearchProjectionFactoryContext<>( delegate.getSearchProjectionFactory() );
	}
}
