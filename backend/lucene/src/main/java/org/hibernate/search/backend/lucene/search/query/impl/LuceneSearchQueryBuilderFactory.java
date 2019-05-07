/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchScopeModel;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneCompositeListProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneObjectProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneReferenceProjection;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

public class LuceneSearchQueryBuilderFactory
		implements SearchQueryBuilderFactory<LuceneSearchQueryElementCollector> {

	private final SearchBackendContext searchBackendContext;

	private final LuceneSearchScopeModel scopeModel;

	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;

	LuceneSearchQueryBuilderFactory(SearchBackendContext searchBackendContext,
			LuceneSearchScopeModel scopeModel,
			LuceneSearchProjectionBuilderFactory searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.scopeModel = scopeModel;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <O> LuceneSearchQueryBuilder<O> asObject(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, O> loadingContextBuilder) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, LuceneObjectProjection.get() );
	}

	@Override
	public <T> LuceneSearchQueryBuilder<T> asReference(
			SessionContextImplementor sessionContext, LoadingContextBuilder<T, ?> loadingContextBuilder) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, LuceneReferenceProjection.get() );
	}

	@Override
	public <T> LuceneSearchQueryBuilder<T> asProjection(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			SearchProjection<T> projection) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder,
				searchProjectionFactory.toImplementation( projection ) );
	}

	@Override
	public LuceneSearchQueryBuilder<List<?>> asProjections(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			SearchProjection<?>... projections) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, createRootProjection( projections ) );
	}

	private LuceneSearchProjection<?, List<?>> createRootProjection(SearchProjection<?>[] projections) {
		List<LuceneSearchProjection<?, ?>> children = new ArrayList<>( projections.length );

		for ( SearchProjection<?> projection : projections ) {
			children.add( searchProjectionFactory.toImplementation( projection ) );
		}

		return new LuceneCompositeListProjection<>( Function.identity(), children );
	}

	private <T> LuceneSearchQueryBuilder<T> createSearchQueryBuilder(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, T> rootProjection) {
		return searchBackendContext.createSearchQueryBuilder(
				scopeModel, sessionContext, loadingContextBuilder, rootProjection
		);
	}
}
