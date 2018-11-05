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
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.dsl.query.SearchQueryResultContext;
import org.hibernate.search.engine.search.loading.spi.ObjectLoader;
import org.hibernate.search.util.impl.integrationtest.common.stub.StubSessionContext;

public final class StubMappingQueryResultDefinitionContext<R, O> {

	private final MappedIndexSearchTarget indexSearchTarget;
	private final StubSessionContext sessionContext;
	private final Function<DocumentReference, R> documentReferenceTransformer;
	private final ObjectLoader<R, O> objectLoader;

	StubMappingQueryResultDefinitionContext(MappedIndexSearchTarget indexSearchTarget,
			StubSessionContext sessionContext,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		this.indexSearchTarget = indexSearchTarget;
		this.sessionContext = sessionContext;
		this.documentReferenceTransformer = documentReferenceTransformer;
		this.objectLoader = objectLoader;
	}

	public SearchQueryResultContext<SearchQuery<O>> asObjects() {
		return asObjects( Function.identity() );
	}

	public <Q> SearchQueryResultContext<Q> asObjects(Function<SearchQuery<O>, Q> searchQueryWrapperFactory) {
		return indexSearchTarget.queryAsLoadedObjects(
				sessionContext, documentReferenceTransformer, objectLoader, searchQueryWrapperFactory
		);
	}

	public SearchQueryResultContext<SearchQuery<R>> asReferences() {
		return asReferences( Function.identity() );
	}

	public <T> SearchQueryResultContext<SearchQuery<T>> asReferences(Function<R, T> hitTransformer) {
		return asReferences( hitTransformer, Function.identity() );
	}

	public <T, Q> SearchQueryResultContext<Q> asReferences(
			Function<R, T> hitTransformer,
			Function<SearchQuery<T>, Q> searchQueryWrapperFactory) {
		return indexSearchTarget.queryAsReferences(
				sessionContext, documentReferenceTransformer, hitTransformer, searchQueryWrapperFactory
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
				sessionContext, documentReferenceTransformer, objectLoader, hitTransformer, searchQueryWrapperFactory,
				projections
		);
	}
}
