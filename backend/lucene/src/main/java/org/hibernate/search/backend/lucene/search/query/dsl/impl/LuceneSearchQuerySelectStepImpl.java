/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.projection.spi.ProjectionAccumulator;
import org.hibernate.search.engine.search.projection.spi.ProjectionCompositor;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;

public class LuceneSearchQuerySelectStepImpl<R, E, LOS>
		extends AbstractSearchQuerySelectStep<
				LuceneSearchQueryOptionsStep<E, LOS>,
				R,
				E,
				LOS,
				LuceneSearchProjectionFactory<R, E>,
				LuceneSearchPredicateFactory>
		implements LuceneSearchQuerySelectStep<R, E, LOS> {

	private final LuceneSearchQueryIndexScope<?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContextBuilder<E, LOS> loadingContextBuilder;

	public LuceneSearchQuerySelectStepImpl(LuceneSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public LuceneSearchQueryWhereStep<E, LOS> selectEntity() {
		return select( scope.<R, E>projectionFactory().entity().toProjection() );
	}

	@Override
	public LuceneSearchQueryWhereStep<R, LOS> selectEntityReference() {
		return select( scope.projectionBuilders().entityReference() );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<P, LOS> select(Class<P> objectClass) {
		return select( scope.projectionFactory().composite().as( objectClass ).toProjection() );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<P, LOS> select(
			Function<? super LuceneSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjection<P> projection = projectionContributor.apply( scope.projectionFactory() ).toProjection();
		return select( projection );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<P, LOS> select(SearchProjection<P> projection) {
		LuceneSearchQueryBuilder<P> builder =
				scope.select( sessionContext, loadingContextBuilder, projection );
		return new LuceneSearchQueryOptionsStepImpl<>( scope, builder, loadingContextBuilder );
	}

	@Override
	public LuceneSearchQueryWhereStep<List<?>, LOS> select(SearchProjection<?>... projections) {
		return select( scope.projectionBuilders().composite()
				.build( projections, ProjectionCompositor.fromList( projections.length ),
						ProjectionAccumulator.single() ) );
	}

	@Override
	public LuceneSearchQueryOptionsStep<E, LOS> where(SearchPredicate predicate) {
		return selectEntity().where( predicate );
	}

	@Override
	public LuceneSearchQueryOptionsStep<E, LOS> where(
			Function<? super LuceneSearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	public LuceneSearchQueryOptionsStep<E, LOS> where(
			BiConsumer<? super LuceneSearchPredicateFactory,
					? super SimpleBooleanPredicateClausesCollector<?>> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	protected LuceneSearchQueryIndexScope<?> scope() {
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

