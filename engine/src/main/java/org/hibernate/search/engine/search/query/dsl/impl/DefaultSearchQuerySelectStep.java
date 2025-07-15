/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query.dsl.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.predicate.dsl.TypedSearchPredicateFactory;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.dsl.TypedSearchProjectionFactory;
import org.hibernate.search.engine.search.query.dsl.SearchQueryOptionsStep;
import org.hibernate.search.engine.search.query.dsl.SearchQueryWhereStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

public final class DefaultSearchQuerySelectStep<SR, R, E, LOS>
		extends AbstractSearchQuerySelectStep<
				SR,
				SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?>,
				R,
				E,
				LOS,
				TypedSearchProjectionFactory<SR, R, E>,
				TypedSearchPredicateFactory<SR>> {

	private final SearchQueryIndexScope<SR, ?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContextBuilder<E, LOS> loadingContextBuilder;

	public DefaultSearchQuerySelectStep(SearchQueryIndexScope<SR, ?> scope, BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public DefaultSearchQueryOptionsStep<SR, E, LOS> selectEntity() {
		return select( scope.<R, E>projectionFactory().entity().toProjection() );
	}

	@Override
	public DefaultSearchQueryOptionsStep<SR, R, LOS> selectEntityReference() {
		return select( scope.projectionBuilders().entityReference() );
	}

	@Override
	public <P> SearchQueryWhereStep<SR, ?, P, LOS, ?> select(Class<P> objectClass) {
		return select( scope.projectionFactory().composite().as( objectClass ).toProjection() );
	}

	@Override
	public <P> DefaultSearchQueryOptionsStep<SR, P, LOS> select(
			Function<? super TypedSearchProjectionFactory<SR, R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjection<P> projection = projectionContributor.apply( scope.projectionFactory() ).toProjection();
		return select( projection );
	}

	@Override
	public <P> DefaultSearchQueryOptionsStep<SR, P, LOS> select(SearchProjection<P> projection) {
		SearchQueryBuilder<P> builder = scope.select( sessionContext, loadingContextBuilder, projection );
		return new DefaultSearchQueryOptionsStep<>( scope, builder, loadingContextBuilder );
	}

	@Override
	public DefaultSearchQueryOptionsStep<SR, List<?>, LOS> select(SearchProjection<?>... projections) {
		return select( scope.projectionBuilders().composite()
				.build( projections, ResultsCompositor.fromList( projections.length ),
						ProjectionCollector.nullable() ) );
	}

	@Override
	public SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?> where(
			Function<? super TypedSearchPredicateFactory<SR>, ? extends PredicateFinalStep> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	public SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?> where(SearchPredicate predicate) {
		return selectEntity().where( predicate );
	}

	@Override
	public SearchQueryOptionsStep<SR, ?, E, LOS, ?, ?> where(
			BiConsumer<? super TypedSearchPredicateFactory<SR>,
					? super SimpleBooleanPredicateClausesCollector<SR, ?>> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	protected SearchQueryIndexScope<SR, ?> scope() {
		return scope;
	}

	@Override
	protected BackendSessionContext sessionContext() {
		return sessionContext;
	}

	@Override
	protected SearchLoadingContextBuilder<E, LOS> loadingContextBuilder() {
		return loadingContextBuilder;
	}
}
