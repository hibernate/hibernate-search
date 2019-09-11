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
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryPredicateStep;
import org.hibernate.search.backend.lucene.search.query.dsl.LuceneSearchQueryHitTypeStep;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;
import org.hibernate.search.engine.search.projection.dsl.ProjectionFinalStep;
import org.hibernate.search.engine.search.query.dsl.spi.AbstractSearchQueryHitTypeStep;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class LuceneSearchQueryHitTypeStepImpl<R, E>
		extends AbstractSearchQueryHitTypeStep<
		LuceneSearchQueryOptionsStep<E>,
						R,
						E,
				LuceneSearchProjectionFactory<R, E>,
						LuceneSearchPredicateFactory,
						LuceneSearchQueryElementCollector
				>
		implements LuceneSearchQueryHitTypeStep<R, E> {

	private final LuceneIndexScope indexScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public LuceneSearchQueryHitTypeStepImpl(LuceneIndexScope indexScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public LuceneSearchQueryPredicateStep<E> asEntity() {
		LuceneSearchQueryBuilder<E> builder = indexScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryPredicateStep<R> asEntityReference() {
		LuceneSearchQueryBuilder<R> builder = indexScope.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public <P> LuceneSearchQueryPredicateStep<P> asProjection(
			Function<? super LuceneSearchProjectionFactory<R, E>, ? extends ProjectionFinalStep<P>> projectionContributor) {
		LuceneSearchProjectionFactory<R, E> factoryContext =
				createDefaultProjectionFactory().extension( LuceneExtension.get() );
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return asProjection( projection );
	}

	@Override
	public <P> LuceneSearchQueryPredicateStep<P> asProjection(SearchProjection<P> projection) {
		LuceneSearchQueryBuilder<P> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryPredicateStep<List<?>> asProjections(SearchProjection<?>... projections) {
		LuceneSearchQueryBuilder<List<?>> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryOptionsStep<E> predicate(SearchPredicate predicate) {
		return asEntity().predicate( predicate );
	}

	@Override
	public LuceneSearchQueryOptionsStep<E> predicate(
			Function<? super LuceneSearchPredicateFactory, ? extends PredicateFinalStep> predicateContributor) {
		return asEntity().predicate( predicateContributor );
	}

	@Override
	protected LuceneIndexScope getIndexScope() {
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

	private <H> LuceneSearchQueryPredicateStep<H> createSearchQueryContext(LuceneSearchQueryBuilder<H> builder) {
		return new LuceneSearchQueryOptionsStepImpl<>( indexScope, builder );
	}
}

