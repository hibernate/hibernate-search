/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.stub.mapper;

import java.util.List;
import java.util.function.Function;

import org.hibernate.search.engine.mapper.mapping.spi.MappedIndexSearchTarget;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

public final class StubMappingQueryResultDefinitionContext<R, O> {

	private final MappedIndexSearchTarget<R, O> indexSearchTarget;
	private final StubSessionContext sessionContext;
	private final ObjectLoader<R, O> objectLoader;

	StubMappingQueryResultDefinitionContext(MappedIndexSearchTarget<R, O> indexSearchTarget,
			StubSessionContext sessionContext,
			ObjectLoader<R, O> objectLoader) {
		this.indexSearchTarget = indexSearchTarget;
		this.sessionContext = sessionContext;
		this.objectLoader = objectLoader;
	}

	public SearchQueryResultContext<SearchQuery<O>> asObject() {
		return asObject( Function.identity() );
	}

	public <Q> SearchQueryResultContext<Q> asObject(Function<SearchQuery<O>, Q> searchQueryWrapperFactory) {
		return indexSearchTarget.queryAsLoadedObject(
				sessionContext, objectLoader, searchQueryWrapperFactory
		);
	}

	public SearchQueryResultContext<SearchQuery<R>> asReference() {
		return asReference( Function.identity() );
	}

	public <T> SearchQueryResultContext<SearchQuery<T>> asReference(Function<R, T> hitTransformer) {
		return asReference( hitTransformer, Function.identity() );
	}

	public <T, Q> SearchQueryResultContext<Q> asReference(
			Function<R, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return indexSearchTarget.queryAsReference(
				sessionContext, hitTransformer, searchQueryWrapperFactory
		);
	}

	public <P> SearchQueryResultContext<SearchQuery<P>> asProjection(SearchProjection<P> projection) {
		return asProjection( Function.identity(), projection );
	}

	public <P, T> SearchQueryResultContext<SearchQuery<T>> asProjection(Function<P, T> hitTransformer,
			SearchProjection<P> projection) {
		return asProjection(
				hitTransformer, Function.identity(),
				projection
		);
	}

	public <P, T, Q> SearchQueryResultContext<Q> asProjection(Function<P, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<P> projection) {
		return indexSearchTarget.queryAsProjection(
				sessionContext, objectLoader, hitTransformer, searchQueryWrapperFactory,
				projection
		);
	}

	public SearchQueryResultContext<SearchQuery<List<?>>> asProjections(SearchProjection<?>... projections) {
		return asProjections( Function.identity(), projections );
	}

	public <T> SearchQueryResultContext<SearchQuery<T>> asProjections(Function<List<?>, T> hitTransformer,
			SearchProjection<?>... projections) {
		return asProjections(
				hitTransformer, Function.identity(),
				projections
		);
	}

	public <T, Q> SearchQueryResultContext<Q> asProjections(Function<List<?>, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory,
			SearchProjection<?>... projections) {
		return indexSearchTarget.queryAsProjections(
				sessionContext, objectLoader, hitTransformer, searchQueryWrapperFactory,
				projections
		);
	}
}
