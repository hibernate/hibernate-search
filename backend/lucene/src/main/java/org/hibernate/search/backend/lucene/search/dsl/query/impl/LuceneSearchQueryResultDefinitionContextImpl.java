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
import org.hibernate.search.backend.lucene.search.dsl.projection.LuceneSearchProjectionFactoryContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryResultContext;
import org.hibernate.search.backend.lucene.search.dsl.query.LuceneSearchQueryResultDefinitionContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneIndexSearchScope;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class LuceneSearchQueryResultDefinitionContextImpl<R, E>
		extends AbstractSearchQueryResultDefinitionContext<
				R,
				E,
				LuceneSearchProjectionFactoryContext<R, E>,
				LuceneSearchQueryElementCollector
		>
		implements LuceneSearchQueryResultDefinitionContext<R, E> {

	private final LuceneIndexSearchScope indexSearchScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public LuceneSearchQueryResultDefinitionContextImpl(LuceneIndexSearchScope indexSearchScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexSearchScope = indexSearchScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public LuceneSearchQueryResultContext<E> asEntity() {
		LuceneSearchQueryBuilder<E> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryResultContext<R> asReference() {
		LuceneSearchQueryBuilder<R> builder = indexSearchScope.getSearchQueryBuilderFactory()
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
		LuceneSearchQueryBuilder<P> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public LuceneSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections) {
		LuceneSearchQueryBuilder<List<?>> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
	}

	@Override
	protected LuceneIndexSearchScope getIndexSearchScope() {
		return indexSearchScope;
	}

	@Override
	protected SessionContextImplementor getSessionContext() {
		return sessionContext;
	}

	@Override
	protected LoadingContextBuilder<R, E> getLoadingContextBuilder() {
		return loadingContextBuilder;
	}

	private <T> LuceneSearchQueryResultContext<T> createSearchQueryContext(LuceneSearchQueryBuilder<T> builder) {
		return new LuceneSearchQueryContextImpl<>( indexSearchScope, builder );
	}
}

