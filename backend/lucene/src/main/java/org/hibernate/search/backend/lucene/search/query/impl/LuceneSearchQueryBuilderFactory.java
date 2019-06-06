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

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneCompositeListProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneEntityProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneReferenceProjection;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

public class LuceneSearchQueryBuilderFactory
		implements SearchQueryBuilderFactory<LuceneSearchQueryElementCollector> {

	private final SearchBackendContext searchBackendContext;

	private final LuceneSearchContext searchContext;

	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;

	public LuceneSearchQueryBuilderFactory(SearchBackendContext searchBackendContext,
			LuceneSearchContext searchContext,
			LuceneSearchProjectionBuilderFactory searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.searchContext = searchContext;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <E> LuceneSearchQueryBuilder<E> asEntity(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, E> loadingContextBuilder) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, LuceneEntityProjection.get() );
	}

	@Override
	public <R> LuceneSearchQueryBuilder<R> asReference(
			SessionContextImplementor sessionContext, LoadingContextBuilder<R, ?> loadingContextBuilder) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, LuceneReferenceProjection.get() );
	}

	@Override
	public <P> LuceneSearchQueryBuilder<P> asProjection(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			SearchProjection<P> projection) {
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

	private <H> LuceneSearchQueryBuilder<H> createSearchQueryBuilder(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, H> rootProjection) {
		return searchBackendContext.createSearchQueryBuilder(
				searchContext, sessionContext, loadingContextBuilder, rootProjection
		);
	}
}
