/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.dsl.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.predicate.dsl.LuceneSearchPredicateFactory;
import org.hibernate.search.backend.lucene.search.projection.dsl.LuceneSearchProjectionFactory;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryOptionsStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryWhereStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQuerySelectStep;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQuerySelectStep;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class LuceneSearchQuerySelectStepImpl<R, E, LOS>
		extends AbstractSearchQuerySelectStep<
						LuceneSearchQueryOptionsStep<E, LOS>,
						R,
						E,
						LOS,
						LuceneSearchProjectionFactory<R, E>,
						LuceneSearchPredicateFactory,
						LuceneSearchQueryElementCollector
				>
		implements LuceneSearchQuerySelectStep<R, E, LOS> {

	private final LuceneIndexScope indexScope;
	private final BackendSessionContext sessionContext;
	private final LoadingContextBuilder<R, E, LOS> loadingContextBuilder;

	public LuceneSearchQuerySelectStepImpl(LuceneIndexScope indexScope,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<R, E, LOS> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public LuceneSearchQueryWhereStep<E, LOS> selectEntity() {
		LuceneSearchQueryBuilder<E> builder = indexScope.searchQueryBuilderFactory()
				.selectEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryWhereStep<R, LOS> selectEntityReference() {
		LuceneSearchQueryBuilder<R> builder = indexScope.searchQueryBuilderFactory()
				.selectEntityReference( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<P, LOS> select(
			Function<? super LuceneSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		LuceneSearchProjectionFactory<R, E> factoryContext =
				createDefaultProjectionFactory().extension( LuceneExtension.get() );
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return select( projection );
	}

	@Override
	public <P> LuceneSearchQueryWhereStep<P, LOS> select(SearchProjection<P> projection) {
		LuceneSearchQueryBuilder<P> builder = indexScope.searchQueryBuilderFactory()
				.select( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryWhereStep<List<?>, LOS> select(SearchProjection<?>... projections) {
		LuceneSearchQueryBuilder<List<?>> builder = indexScope.searchQueryBuilderFactory()
				.select( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
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
	protected LuceneIndexScope indexScope() {
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

	private <H> LuceneSearchQueryWhereStep<H, LOS> createSearchQueryContext(LuceneSearchQueryBuilder<H> builder) {
		return new LuceneSearchQueryOptionsStepImpl<>( indexScope, builder, loadingContextBuilder );
	}
}

