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
import org.hibernate.search.backend.elasticsearch.search.dsl.projection.ElasticsearchSearchProjectionFactoryContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultContext;
import org.hibernate.search.backend.elasticsearch.search.dsl.query.ElasticsearchSearchQueryResultDefinitionContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchIndexSearchScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionTerminalContext;
import org.hibernate.search.engine.search.dsl.query.spi.AbstractSearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;

public class ElasticsearchSearchQueryResultDefinitionContextImpl<R, E>
		extends AbstractSearchQueryResultDefinitionContext<
				R,
				E,
				ElasticsearchSearchProjectionFactoryContext<R, E>,
				ElasticsearchSearchQueryElementCollector
		>
		implements ElasticsearchSearchQueryResultDefinitionContext<R, E> {

	private final ElasticsearchIndexSearchScope indexSearchScope;
	private final SessionContextImplementor sessionContext;
	private final LoadingContextBuilder<R, E> loadingContextBuilder;

	public ElasticsearchSearchQueryResultDefinitionContextImpl(ElasticsearchIndexSearchScope indexSearchScope,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<R, E> loadingContextBuilder) {
		this.indexSearchScope = indexSearchScope;
		this.sessionContext = sessionContext;
		this.loadingContextBuilder = loadingContextBuilder;
	}

	@Override
	public ElasticsearchSearchQueryResultContext<E> asEntity() {
		ElasticsearchSearchQueryBuilder<E> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asEntity( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryResultContext<R> asReference() {
		ElasticsearchSearchQueryBuilder<R> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asReference( sessionContext, loadingContextBuilder );
		return createSearchQueryContext( builder );
	}

	@Override
	public <P> ElasticsearchSearchQueryResultContext<P> asProjection(
			Function<? super ElasticsearchSearchProjectionFactoryContext<R, E>, ? extends SearchProjectionTerminalContext<P>> projectionContributor) {
		ElasticsearchSearchProjectionFactoryContext<R, E> factoryContext =
				createDefaultProjectionFactoryContext().extension( ElasticsearchExtension.get() );
		SearchProjection<P> projection = projectionContributor.apply( factoryContext ).toProjection();
		return asProjection( projection );
	}

	@Override
	public <P> ElasticsearchSearchQueryResultContext<P> asProjection(SearchProjection<P> projection) {
		ElasticsearchSearchQueryBuilder<P> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, loadingContextBuilder, projection );
		return createSearchQueryContext( builder );
	}

	@Override
	public ElasticsearchSearchQueryResultContext<List<?>> asProjections(SearchProjection<?>... projections) {
		ElasticsearchSearchQueryBuilder<List<?>> builder = indexSearchScope.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, loadingContextBuilder, projections );
		return createSearchQueryContext( builder );
	}

	@Override
	protected ElasticsearchIndexSearchScope getIndexSearchScope() {
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

	private <T> ElasticsearchSearchQueryResultContext<T> createSearchQueryContext(ElasticsearchSearchQueryBuilder<T> builder) {
		return new ElasticsearchSearchQueryContextImpl<>( indexSearchScope, builder );
	}
}

