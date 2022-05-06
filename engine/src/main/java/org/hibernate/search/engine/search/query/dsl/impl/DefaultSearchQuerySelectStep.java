/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.dsl.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateOptionsCollector;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SearchPredicateFactory;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.SearchProjectionFactory;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;

public final class DefaultSearchQuerySelectStep<R, E, LOS>
		extends AbstractSearchQuerySelectStep<
								SearchQueryOptionsStep<?, E, LOS, ?, ?>,
								R,
								E,
								LOS,
								SearchProjectionFactory<R, E>,
								SearchPredicateFactory
						> {

	private final SearchQueryIndexScope<?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContextBuilder<R, E, LOS> loadingContextBuilder;

	public DefaultSearchQuerySelectStep(SearchQueryIndexScope<?> scope, BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public DefaultSearchQueryOptionsStep<E, LOS> selectEntity() {
		return select( scope.projectionBuilders().entity() );
	}

	@Override
	public DefaultSearchQueryOptionsStep<R, LOS> selectEntityReference() {
		return select( scope.projectionBuilders().entityReference() );
	}

	@Override
	public <P> DefaultSearchQueryOptionsStep<P, LOS> select(
			Function<? super SearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjection<P> projection = projectionContributor.apply( scope.projectionFactory() ).toProjection();
		return select( projection );
	}

	@Override
	public <P> DefaultSearchQueryOptionsStep<P, LOS> select(SearchProjection<P> projection) {
		SearchQueryBuilder<P> builder = scope.select( sessionContext, loadingContextBuilder, projection );
		return new DefaultSearchQueryOptionsStep<>( scope, builder, loadingContextBuilder );
	}

	@Override
	public DefaultSearchQueryOptionsStep<List<?>, LOS> select(SearchProjection<?>... projections) {
		return select( scope.projectionBuilders().composite()
				.build( projections, ProjectionCompositor.fromList( projections.length ),
						ProjectionAccumulator.single() ) );
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
	public SearchQueryOptionsStep<?, E, LOS, ?, ?> where(
			BiConsumer<? super SearchPredicateFactory, ? super BooleanPredicateOptionsCollector<?>> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	protected SearchQueryIndexScope<?> scope() {
		return scope;
	}

	@Override
	protected BackendSessionContext sessionContext() {
		return sessionContext;
	}

	@Override
	protected SearchLoadingContextBuilder<R, E, LOS> loadingContextBuilder() {
		return loadingContextBuilder;
	}
}
