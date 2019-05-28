/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.dsl.query.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.LuceneExtension;
import org.hibernate.search.backend.lucene.search.dsl.predicate.LuceneSearchPredicateFactoryContext;
import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactoryContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryResultContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryResultDefinitionContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.scope.impl.LuceneIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchPredicate;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateTerminalContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class LuceneSearchQueryResultDefinitionContextImpl<R, E>
		extends AbstractSearchQueryResultDefinitionContext<
				LuceneSearchQueryContext<E>,
				R,
				E,
				LuceneSearchProjectionFactoryContext<R, E>,
				LuceneSearchPredicateFactoryContext,
				LuceneSearchQueryElementCollector
		>
		implements LuceneSearchQueryResultDefinitionContext<R, E> {

	private final LuceneIndexScope indexScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public LuceneSearchQueryResultDefinitionContextImpl(LuceneIndexScope indexScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexScope = indexScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public LuceneSearchQueryResultContext<E> asEntity() {
		LuceneSearchQueryBuilder<E> builder = indexScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryResultContext<R> asReference() {
		LuceneSearchQueryBuilder<R> builder = indexScope.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public <P> LuceneSearchQueryResultContext<P> asProjection(
			Function<? super LuceneSearchProjectionFactoryContext<R, E>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		LuceneSearchProjectionFactoryContext<R, E> factoryContext =
				createDefaultProjectionFactoryContext().extension( LuceneExtension.get() );
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return asProjection( projection );
	}

	@Override
	public <P> LuceneSearchQueryResultContext<P> asProjection(SearchProjection<P> projection) {
		LuceneSearchQueryBuilder<P> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections) {
		LuceneSearchQueryBuilder<List<?>> builder = indexScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryContext<E> predicate(SearchPredicate predicate) {
		return asEntity().predicate( predicate );
	}

	@Override
	public LuceneSearchQueryContext<E> predicate(
			Function<? super LuceneSearchPredicateFactoryContext, SearchPredicateTerminalContext> predicateContributor) {
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

	private <H> LuceneSearchQueryResultContext<H> createSearchQueryContext(LuceneSearchQueryBuilder<H> builder) {
		return new LuceneSearchQueryContextImpl<>( indexScope, builder );
	}
}

