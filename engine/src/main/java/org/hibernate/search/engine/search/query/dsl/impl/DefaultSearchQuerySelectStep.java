/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;
import org.hibernate.search.engine.backend.scope.spi.IndexScope;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

public final class DefaultSearchQuerySelectStep<R, E, LOS, C>
		extends AbstractSearchQuerySelectStep<
								SearchQueryOptionsStep<?, E, LOS, ?, ?>,
								R,
								E,
								LOS,
								SearchProjectionFactory<R, E>,
								SearchPredicateFactory,
								C
						> {

	private final IndexScope<C> indexScope;
	private final BackendSessionContext sessionContext;
	private final LoadingContextBuilder<R, E, LOS> loadingContextBuilder;

	public DefaultSearchQuerySelectStep(IndexScope<C> indexScope,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public DefaultSearchQueryOptionsStep<E, LOS, C> selectEntity() {
		SearchQueryBuilder<E, C> builder = indexScope.searchQueryBuilderFactory()
				.selectEntity( sessionContext, loadingContextBuilder );
		return new DefaultSearchQueryOptionsStep<E, LOS, C>( indexScope, builder, loadingContextBuilder );
	}

	@Override
	public DefaultSearchQueryOptionsStep<R, LOS, C> selectEntityReference() {
		SearchQueryBuilder<R, C> builder = indexScope.searchQueryBuilderFactory()
				.selectEntityReference( sessionContext, loadingContextBuilder );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder, loadingContextBuilder );
	}

	@Override
	public <P> DefaultSearchQueryOptionsStep<P, LOS, C> select(
			Function<? super SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjectionFactory<R, E> factory = createDefaultProjectionFactory();
		SearchProjection<P> projection = projectionContributor.apply( factory ).toProjection();
		return select( projection );
	}

	@Override
	public <P> DefaultSearchQueryOptionsStep<P, LOS, C> select(SearchProjection<P> projection) {
		SearchQueryBuilder<P, C> builder = indexScope.searchQueryBuilderFactory()
				.select( sessionContext, loadingContextBuilder, projection );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder, loadingContextBuilder );
	}

	@Override
	public DefaultSearchQueryOptionsStep<List<?>, LOS, C> select(SearchProjection<?>... projections) {
		SearchQueryBuilder<List<?>, C> builder = indexScope.searchQueryBuilderFactory()
				.select( sessionContext, loadingContextBuilder, projections );
		return new DefaultSearchQueryOptionsStep<>( indexScope, builder, loadingContextBuilder );
	}

	@Override
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(
			Function<? super SearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(SearchPredicate predicate) {
		return selectEntity().where( predicate );
	}

	@Override
	protected IndexScope<C> indexScope() {
		return indexScope;
	}

	@Override
	protected BackendSessionContext sessionContext() {
		return sessionContext;
	}

	@Override
	protected LoadingContextBuilder<R, E, LOS> loadingContextBuilder() {
		return loadingContextBuilder;
	}
}
