/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.common.impl;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.predicate.SearchPredicateFactoryContext;
import org.hibernate.search.engine.search.dsl.predicate.impl.SearchPredicateFactoryContextImpl;
import org.hibernate.search.engine.search.dsl.projection.SearchProjectionFactoryContext;
import org.hibernate.search.engine.search.dsl.projection.impl.SearchProjectionFactoryContextImpl;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.dsl.query.impl.SearchQueryResultContextImpl;
import org.hibernate.search.engine.search.dsl.sort.SearchSortContainerContext;
import org.hibernate.search.engine.search.dsl.sort.impl.SearchTargetSortRootContext;
import org.hibernate.search.engine.search.dsl.spi.SearchTargetContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.engine.search.query.impl.ProjectionHitMapperImpl;
import org.hibernate.search.engine.search.query.impl.ReferenceHitMapperImpl;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;

class MappedIndexSearchTargetImpl<C, R, O> implements MappedIndexSearchTarget<R, O> {

	private final SearchTargetContext<C> searchTargetContext;
	private final Function<DocumentReference, R> documentReferenceTransformer;

	MappedIndexSearchTargetImpl(SearchTargetContext<C> searchTargetContext,
			Function<DocumentReference, R> documentReferenceTransformer) {
		this.searchTargetContext = searchTargetContext;
		this.documentReferenceTransformer = documentReferenceTransformer;
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "context=" ).append( searchTargetContext )
				.append( "]" )
				.toString();
	}

	@Override
	public <T, Q> SearchQueryResultContext<Q> queryAsLoadedObject(SessionContextImplementor sessionContext,
			ObjectLoader<R, T> objectLoader,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		ProjectionHitMapper<R, T> projectionHitMapper =
				new ProjectionHitMapperImpl<>( documentReferenceTransformer, objectLoader );

		SearchQueryBuilder<T, C> builder = searchTargetContext.getSearchQueryBuilderFactory()
				.asObject( sessionContext, projectionHitMapper );

		return new SearchQueryResultContextImpl<>(
				searchTargetContext, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <Q> SearchQueryResultContext<Q> queryAsReference(SessionContextImplementor sessionContext,
			Function<SearchQuery<R>, Q> searchQueryWrapperFactory) {
		ProjectionHitMapper<R, Void> referenceHitMapper = new ReferenceHitMapperImpl<>( documentReferenceTransformer );

		SearchQueryBuilder<R, C> builder = searchTargetContext.getSearchQueryBuilderFactory()
				.asReference( sessionContext, referenceHitMapper );

		return new SearchQueryResultContextImpl<>(
				searchTargetContext, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <T, Q> SearchQueryResultContext<Q> queryAsProjection(SessionContextImplementor sessionContext,
			ObjectLoader<R, O> objectLoader,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory, SearchProjection<T> projection) {
		ProjectionHitMapper<R, O> projectionHitMapper =
				new ProjectionHitMapperImpl<>( documentReferenceTransformer, objectLoader );

		SearchQueryBuilder<T, C> builder = searchTargetContext.getSearchQueryBuilderFactory()
				.asProjection( sessionContext, projectionHitMapper, projection );

		return new SearchQueryResultContextImpl<>(
				searchTargetContext, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public <Q> SearchQueryResultContext<Q> queryAsProjections(
			SessionContextImplementor sessionContext,
			ObjectLoader<R, O> objectLoader,
			Function<SearchQuery<List<?>>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections) {
		ProjectionHitMapper<R, O> projectionHitMapper =
				new ProjectionHitMapperImpl<>( documentReferenceTransformer, objectLoader );

		SearchQueryBuilder<List<?>, C> builder = searchTargetContext.getSearchQueryBuilderFactory()
				.asProjections( sessionContext, projectionHitMapper, projections );

		return new SearchQueryResultContextImpl<>(
				searchTargetContext, builder, searchQueryWrapperFactory
		);
	}

	@Override
	public SearchPredicateFactoryContext predicate() {
		return new SearchPredicateFactoryContextImpl<>( searchTargetContext.getSearchPredicateBuilderFactory() );
	}

	@Override
	public SearchSortContainerContext sort() {
		return new SearchTargetSortRootContext<>( searchTargetContext.getSearchSortBuilderFactory() );
	}

	@Override
	public SearchProjectionFactoryContext<R, O> projection() {
		return new SearchProjectionFactoryContextImpl<>( searchTargetContext.getSearchProjectionFactory() );
	}
}
