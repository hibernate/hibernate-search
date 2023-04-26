/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl.impl;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQuerySelectStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryWhereStep;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
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
import org.hibernate.search.engine.search.query.spi.SearchQueryIndexScope;

public class ElasticsearchSearchQuerySelectStepImpl<R, E, LOS>
		extends AbstractSearchQuerySelectStep<
						ElasticsearchSearchQueryOptionsStep<E, LOS>,
						R,
						E,
						LOS,
						ElasticsearchSearchProjectionFactory<R, E>,
						ElasticsearchSearchPredicateFactory
				>
		implements ElasticsearchSearchQuerySelectStep<R, E, LOS> {

	private final ElasticsearchSearchQueryIndexScope<?> scope;
	private final BackendSessionContext sessionContext;
	private final SearchLoadingContextBuilder<E, LOS> loadingContextBuilder;

	public ElasticsearchSearchQuerySelectStepImpl(ElasticsearchSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<E, LOS> loadingContextBuilder) {
		this.scope = scope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public ElasticsearchSearchQueryWhereStep<E, LOS> selectEntity() {
		return select( scope.<R, E>projectionFactory().entity().toProjection() );
	}

	@Override
	public ElasticsearchSearchQueryWhereStep<R, LOS> selectEntityReference() {
		return select( scope.projectionBuilders().entityReference() );
	}

	@Override
	public <P> ElasticsearchSearchQueryWhereStep<P, LOS> select(Class<P> objectClass) {
		return select( scope.projectionFactory().composite().as( objectClass ).toProjection() );
	}

	@Override
	public <P> ElasticsearchSearchQueryWhereStep<P, LOS> select(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		SearchProjection<P> projection = projectionContributor.apply( scope.projectionFactory() ).toProjection();
		return select( projection );
	}

	@Override
	public <P> ElasticsearchSearchQueryWhereStep<P, LOS> select(SearchProjection<P> projection) {
		ElasticsearchSearchQueryBuilder<P> builder =
				scope.select( sessionContext, loadingContextBuilder, projection );
		return new ElasticsearchSearchQueryOptionsStepImpl<>( scope, builder, loadingContextBuilder );
	}

	@Override
	public ElasticsearchSearchQueryWhereStep<List<?>, LOS> select(SearchProjection<?>... projections) {
		return select( scope.projectionBuilders().composite()
				.build( projections, ProjectionCompositor.fromList( projections.length ),
						ProjectionAccumulator.single() ) );
	}

	@Override
	public ElasticsearchSearchQueryOptionsStep<E, LOS> where(SearchPredicate predicate) {
		return selectEntity().where( predicate );
	}

	@Override
	public ElasticsearchSearchQueryOptionsStep<E, LOS> where(
			Function<? super ElasticsearchSearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return selectEntity().where( predicateContributor );
	}

	@Override
	public ElasticsearchSearchQueryOptionsStep<E, LOS> where(
			BiConsumer<? super ElasticsearchSearchPredicateFactory, ? super SimpleBooleanPredicateClausesCollector<?>> predicateContributor) {
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
	protected SearchLoadingContextBuilder<E, LOS> loadingContextBuilder() {
		return loadingContextBuilder;
	}

}

