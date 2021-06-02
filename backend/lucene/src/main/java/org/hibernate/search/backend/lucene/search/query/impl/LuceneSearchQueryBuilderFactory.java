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

import org.hibernate.search.backend.lucene.search.impl.LuceneSearchIndexScope;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneCompositeListProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.search.projection.SearchProjection;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

public class LuceneSearchQueryBuilderFactory
		implements SearchQueryBuilderFactory<LuceneSearchQueryElementCollector> {

	private final SearchBackendContext searchBackendContext;

	private final LuceneSearchIndexScope scope;

	private final LuceneSearchProjectionBuilderFactory searchProjectionFactory;

	public LuceneSearchQueryBuilderFactory(SearchBackendContext searchBackendContext,
			LuceneSearchIndexScope scope,
			LuceneSearchProjectionBuilderFactory searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.scope = scope;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <E> LuceneSearchQueryBuilder<E> selectEntity(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<?, E, ?> loadingContextBuilder) {
		return select( sessionContext, loadingContextBuilder, searchProjectionFactory.<E>entity().build() );
	}

	@Override
	public <R> LuceneSearchQueryBuilder<R> selectEntityReference(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<R, ?, ?> loadingContextBuilder) {
		return select( sessionContext, loadingContextBuilder, searchProjectionFactory.<R>entityReference().build() );
	}

	@Override
	public <P> LuceneSearchQueryBuilder<P> select(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			SearchProjection<P> projection) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder,
				LuceneSearchProjection.from( scope, projection ) );
	}

	@Override
	public LuceneSearchQueryBuilder<List<?>> select(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			SearchProjection<?>... projections) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, createRootProjection( projections ) );
	}

	private LuceneSearchProjection<?, List<?>> createRootProjection(SearchProjection<?>[] projections) {
		List<LuceneSearchProjection<?, ?>> children = new ArrayList<>( projections.length );

		for ( SearchProjection<?> projection : projections ) {
			children.add( LuceneSearchProjection.from( scope, projection ) );
		}

		return new LuceneCompositeListProjection<>( scope, Function.identity(), children );
	}

	private <H> LuceneSearchQueryBuilder<H> createSearchQueryBuilder(
			BackendSessionContext sessionContext, SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, H> rootProjection) {
		return searchBackendContext.createSearchQueryBuilder(
				scope, sessionContext, loadingContextBuilder, rootProjection
		);
	}
}
