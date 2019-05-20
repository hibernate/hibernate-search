/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public final class DefaultSearchQueryResultDefinitionContext<R, E, C>
		extends AbstractSearchQueryResultDefinitionContext<
				SearchQueryContext<?, E, ?>,
				R,
				E,
				SearchProjectionFactoryContext<R, E>,
				SearchPredicateFactoryContext,
				C
		> {

	private final IndexSearchScope<C> indexSearchScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public DefaultSearchQueryResultDefinitionContext(IndexSearchScope<C> indexSearchScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexSearchScope = indexSearchScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public SearchQueryResultContext<?, E, ?> asEntity() {
		SearchQueryBuilder<E, C> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return new DefaultSearchQueryContext<>( indexSearchScope, builder );
	}

	@Override
	public SearchQueryResultContext<?, R, ?> asReference() {
		SearchQueryBuilder<R, C> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );
		return new DefaultSearchQueryContext<>( indexSearchScope, builder );
	}

	@Override
	public <P> SearchQueryResultContext<?, P, ?> asProjection(
			Function<? super SearchProjectionFactoryContext<R, E>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		SearchProjectionFactoryContext<R, E> factoryContext = createDefaultProjectionFactoryContext();
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return asProjection( projection );
	}

	@Override
	public <P> SearchQueryResultContext<?, P, ?> asProjection(SearchProjection<P> projection) {
		SearchQueryBuilder<P, C> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return new DefaultSearchQueryContext<>( indexSearchScope, builder );
	}

	@Override
	public SearchQueryResultContext<?, List<?>, ?> asProjections(SearchProjection<?>... projections) {
		SearchQueryBuilder<List<?>, C> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return new DefaultSearchQueryContext<>( indexSearchScope, builder );
	}

	@Override
	public SearchQueryContext<?, E, ?> predicate(
			Function<? super SearchPredicateFactoryContext, SearchPredicateTerminalContext> predicateContributor) {
		return asEntity().predicate( predicateContributor );
	}

	@Override
	public SearchQueryContext<?, E, ?> predicate(SearchPredicate predicate) {
		return asEntity().predicate( predicate );
	}

	@Override
	protected IndexSearchScope<C> getIndexSearchScope() {
		return indexSearchScope;
	}

	@Override
	protected SessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	@Override
	protected LoadingContextBuilder<R, E> getLoadingContextBuilder() {
		return loadingContextBuilder;
	}
}
