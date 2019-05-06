/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchScope;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.dsl.query.impl.DefaultSearchQueryContext;
import org.hibernate.search.engine.search.query.spi.IndexSearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.DefaultSearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.DefaultSearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.DefaultSearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.spi.IndexSearchScope;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.query.impl.DefaultProjectionHitMapper;
import org.hibernate.search.engine.search.query.impl.NoLoadingProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

class MappedIndexSearchScopeImpl<C, R, O> implements MappedIndexSearchScope<R, O> {

	private final IndexSearchScope<C> delegate;
	private final Function<DocumentReference, R> documentReferenceTransformer;

	MappedIndexSearchScopeImpl(IndexSearchScope<C> delegate,
			Function<DocumentReference, R> documentReferenceTransformer) {
		this.delegate = delegate;
		this.documentReferenceTransformer = documentReferenceTransformer;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "delegate=" ).append( delegate )
				.append( "]" )
				.toString();
	}

	@Override
	public <T, Q> SearchQueryResultContext<?, Q, ?> queryAsLoadedObject(SessionContextImplementor sessionContext,
			ObjectLoader<R, T> objectLoader,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory) {
		ProjectionHitMapper<R, T> projectionHitMapper =
				new DefaultProjectionHitMapper<>( documentReferenceTransformer, objectLoader );

		SearchQueryBuilder<T, C> builder = delegate.getSearchQueryBuilderFactory()
				.asObject( sessionContext, projectionHitMapper );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <Q> SearchQueryResultContext<?, Q, ?> queryAsReference(SessionContextImplementor sessionContext,
			Function<IndexSearchQuery<R>, Q> searchQueryWrapperFactory) {
		ProjectionHitMapper<R, Void> referenceHitMapper = new NoLoadingProjectionHitMapper<>( documentReferenceTransformer );

		SearchQueryBuilder<R, C> builder = delegate.getSearchQueryBuilderFactory()
				.asReference( sessionContext, referenceHitMapper );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <T, Q> SearchQueryResultContext<?, Q, ?> queryAsProjection(SessionContextImplementor sessionContext,
			ObjectLoader<R, O> objectLoader,
			Function<IndexSearchQuery<T>, Q> searchQueryWrapperFactory, SearchProjection<T> projection) {
		ProjectionHitMapper<R, O> projectionHitMapper =
				new DefaultProjectionHitMapper<>( documentReferenceTransformer, objectLoader );

		SearchQueryBuilder<T, C> builder = delegate.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, projectionHitMapper, projection );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <Q> SearchQueryResultContext<?, Q, ?> queryAsProjections(
			SessionContextImplementor sessionContext,
			ObjectLoader<R, O> objectLoader,
			Function<IndexSearchQuery<List<?>>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections) {
		ProjectionHitMapper<R, O> projectionHitMapper =
				new DefaultProjectionHitMapper<>( documentReferenceTransformer, objectLoader );

		SearchQueryBuilder<List<?>, C> builder = delegate.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, projectionHitMapper, projections );

		return new DefaultSearchQueryContext<>(
				delegate, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return new DefaultSearchPredicateFactoryContext<>( delegate.getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortContainerContext sort() {
		return new DefaultSearchSortContainerContext<>( delegate.getSearchSortBuilderFactory() );
	}

	@Override
	public SearchProjectionFactoryContext<R, O> projection() {
		return new DefaultSearchProjectionFactoryContext<>( delegate.getSearchProjectionFactory() );
	}
}
