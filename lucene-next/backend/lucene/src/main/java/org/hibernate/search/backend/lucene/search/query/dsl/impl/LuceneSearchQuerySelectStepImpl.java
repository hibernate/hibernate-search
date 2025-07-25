/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query.dsl.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQuerySelectStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryWhereStep;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.predicate.dsl.SimpleBooleanPredicateClausesCollector;
import org.hibernate.search.engine.search.projection.ProjectionCollector;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;
import org.hibernate.search.engine.search.spi.ResultsCompositor;

public class LuceneSearchQuerySelectStepImpl<SR, R, E, LOS>
		extends AbstractSearchQuerySelectStep<
				SR,
				LuceneSearchQueryOptionsStep<SR, E, LOS>,
				R,
				E,
				LOS,
				LuceneSearchProjectionFactory<SR, R, E>,
				LuceneSearchPredicateFactory<SR>>
		implements LuceneSearchQuerySelectStep<SR, R, E, LOS> {

	private final LuceneSearchQueryIndexScope<SR, ?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContextBuilder<E, LOS> loadingContextBuilder;

	public LuceneSearchQuerySelectStepImpl(LuceneSearchQueryIndexScope<SR, ?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public LuceneSearchQueryWhereStep<SR, E, LOS> selectEntity() {
		return select( scope.<R, E>projectionFactory().entity().toProjection() );
	}

	@Override
	public LuceneSearchQueryWhereStep<SR, R, LOS> selectEntityReference() {
		return select( scope.projectionBuilders().entityReference() );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<SR, P, LOS> select(Class<P> objectClass) {
		return select( scope.projectionFactory().composite().as( objectClass ).toProjection() );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<SR, P, LOS> select(
			Function<? super LuceneSearchProjectionFactory<SR, R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjection<P> projection = projectionContributor.apply( scope.projectionFactory() ).toProjection();
		return select( projection );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<SR, P, LOS> select(SearchProjection<P> projection) {
		LuceneSearchQueryBuilder<P> builder =
				scope.select( sessionContext, loadingContextBuilder, projection );
		return new LuceneSearchQueryOptionsStepImpl<>( scope, builder, loadingContextBuilder );
	}

	@Override
	public LuceneSearchQueryWhereStep<SR, List<?>, LOS> select(SearchProjection<?>... projections) {
		return select( scope.projectionBuilders().composite()
				.build( projections, ResultsCompositor.fromList( projections.length ),
						ProjectionCollector.nullable() ) );
	}

	@Override
	public LuceneSearchQueryOptionsStep<SR, E, LOS> where(SearchPredicate predicate) {
		return selectEntity().where( predicate );
	}

	@Override
	public LuceneSearchQueryOptionsStep<SR, E, LOS> where(
			Function<? super LuceneSearchPredicateFactory<SR>, ? extends PredicateFinalStep> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	public LuceneSearchQueryOptionsStep<SR, E, LOS> where(
			BiConsumer<? super LuceneSearchPredicateFactory<SR>,
					? super SimpleBooleanPredicateClausesCollector<SR, ?>> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	protected LuceneSearchQueryIndexScope<SR, ?> scope() {
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
