/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.dsl.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.dsl.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryOptionsStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQueryWhereStep;
import org.hibernate.search.backend.elasticsearch.search.query.dsl.ElasticsearchSearchQuerySelectStep;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class ElasticsearchSearchQuerySelectStepImpl<R, E, LOS>
		extends AbstractSearchQuerySelectStep<
						ElasticsearchSearchQueryOptionsStep<E, LOS>,
						R,
						E,
						LOS,
						ElasticsearchSearchProjectionFactory<R, E>,
						ElasticsearchSearchPredicateFactory,
						ElasticsearchSearchQueryElementCollector
				>
		implements ElasticsearchSearchQuerySelectStep<R, E, LOS> {

	private final ElasticsearchIndexScope indexScope;
	private final BackendSessionContext sessionContext;
	private final LoadingContextBuilder<R, E, LOS> loadingContextBuilder;

	public ElasticsearchSearchQuerySelectStepImpl(ElasticsearchIndexScope indexScope,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public ElasticsearchSearchQueryWhereStep<E, LOS> selectEntity() {
		ElasticsearchSearchQueryBuilder<E> builder = indexScope.searchQueryBuilderFactory()
				.selectEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryWhereStep<R, LOS> selectEntityReference() {
		ElasticsearchSearchQueryBuilder<R> builder = indexScope.searchQueryBuilderFactory()
				.selectEntityReference( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public <P> ElasticsearchSearchQueryWhereStep<P, LOS> select(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		ElasticsearchSearchProjectionFactory<R, E> factoryContext =
				createDefaultProjectionFactory().extension( ElasticsearchExtension.get() );
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return select( projection );
	}

	@Override
	public <P> ElasticsearchSearchQueryWhereStep<P, LOS> select(SearchProjection<P> projection) {
		ElasticsearchSearchQueryBuilder<P> builder = indexScope.searchQueryBuilderFactory()
				.select( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryWhereStep<List<?>, LOS> select(SearchProjection<?>... projections) {
		ElasticsearchSearchQueryBuilder<List<?>> builder = indexScope.searchQueryBuilderFactory()
				.select( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
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
	protected ElasticsearchIndexScope indexScope() {
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

	private <H> ElasticsearchSearchQueryWhereStep<H, LOS> createSearchQueryContext(ElasticsearchSearchQueryBuilder<H> builder) {
		return new ElasticsearchSearchQueryOptionsStepImpl<>( indexScope, builder, loadingContextBuilder );
	}
}

