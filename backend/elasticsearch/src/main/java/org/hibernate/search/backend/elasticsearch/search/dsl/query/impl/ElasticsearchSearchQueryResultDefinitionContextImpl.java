/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.ElasticsearchExtension;
import org.hibernate.search.backend.elasticsearch.search.dsl.predicate.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactory;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultDefinitionContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.scope.impl.ElasticsearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.predicate.PredicateFinalStep;
import org.hibernate.search.engine.search.dsl.projection.ProjectionFinalStep;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class ElasticsearchSearchQueryResultDefinitionContextImpl<R, E>
		extends AbstractSearchQueryResultDefinitionContext<
				ElasticsearchSearchQueryContext<E>,
				R,
				E,
		ElasticsearchSearchProjectionFactory<R, E>,
				ElasticsearchSearchPredicateFactory,
				ElasticsearchSearchQueryElementCollector
		>
		implements ElasticsearchSearchQueryResultDefinitionContext<R, E> {

	private final ElasticsearchIndexScope indexScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public ElasticsearchSearchQueryResultDefinitionContextImpl(ElasticsearchIndexScope indexScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public ElasticsearchSearchQueryResultContext<E> asEntity() {
		ElasticsearchSearchQueryBuilder<E> builder = indexScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryResultContext<R> asEntityReference() {
		ElasticsearchSearchQueryBuilder<R> builder = indexScope.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public <P> ElasticsearchSearchQueryResultContext<P> asProjection(
			Function<? super ElasticsearchSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		ElasticsearchSearchProjectionFactory<R, E> factoryContext =
				createDefaultProjectionFactory().extension( ElasticsearchExtension.get() );
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return asProjection( projection );
	}

	@Override
	public <P> ElasticsearchSearchQueryResultContext<P> asProjection(SearchProjection<P> projection) {
		ElasticsearchSearchQueryBuilder<P> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections) {
		ElasticsearchSearchQueryBuilder<List<?>> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryContext<E> predicate(SearchPredicate predicate) {
		return asEntity().predicate( predicate );
	}

	@Override
	public ElasticsearchSearchQueryContext<E> predicate(
			Function<? super ElasticsearchSearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return asEntity().predicate( predicateContributor );
	}

	@Override
	protected ElasticsearchIndexScope getIndexScope() {
		return indexScope;
	}

	@Override
	protected SessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	@Override
	protected LoadingContextBuilder<R, E> getLoadingContextBuilder() {
		return loadingContextBuilder;
	}

	private <H> ElasticsearchSearchQueryResultContext<H> createSearchQueryContext(ElasticsearchSearchQueryBuilder<H> builder) {
		return new ElasticsearchSearchQueryContextImpl<>( indexScope, builder );
	}
}

