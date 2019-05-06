/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchScopeModel;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchCompositeListProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchObjectProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchReferenceProjection;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

class ElasticsearchSearchQueryBuilderFactory
		implements SearchQueryBuilderFactory<ElasticsearchSearchQueryElementCollector> {

	private final SearchBackendContext searchBackendContext;

	private final ElasticsearchSearchScopeModel scopeModel;

	private final ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory;

	ElasticsearchSearchQueryBuilderFactory(SearchBackendContext searchBackendContext, ElasticsearchSearchScopeModel scopeModel,
			ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.scopeModel = scopeModel;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <O> SearchQueryBuilder<O, ElasticsearchSearchQueryElementCollector> asObject(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, O> loadingContextBuilder) {
		return createSearchQueryBuilder(
				sessionContext, loadingContextBuilder,
				new ElasticsearchObjectProjection<>( searchBackendContext.getDocumentReferenceExtractorHelper() )
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> asReference(
			SessionContextImplementor sessionContext, LoadingContextBuilder<T, ?> loadingContextBuilder) {
		return createSearchQueryBuilder(
				sessionContext, loadingContextBuilder,
				new ElasticsearchReferenceProjection<>( searchBackendContext.getDocumentReferenceExtractorHelper() )
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> asProjection(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			SearchProjection<T> projection) {
		return createSearchQueryBuilder(
				sessionContext, loadingContextBuilder,
				searchProjectionFactory.toImplementation( projection )
		);
	}

	@Override
	public SearchQueryBuilder<List<?>, ElasticsearchSearchQueryElementCollector> asProjections(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			SearchProjection<?>... projections) {
		return createSearchQueryBuilder( sessionContext, loadingContextBuilder, createRootProjection( projections ) );
	}

	private ElasticsearchSearchProjection<?, List<?>> createRootProjection(SearchProjection<?>[] projections) {
		List<ElasticsearchSearchProjection<?, ?>> children = new ArrayList<>( projections.length );

		for ( SearchProjection<?> projection : projections ) {
			children.add( searchProjectionFactory.toImplementation( projection ) );
		}

		return new ElasticsearchCompositeListProjection<>( Function.identity(), children );
	}

	private <T> ElasticsearchSearchQueryBuilder<T> createSearchQueryBuilder(
			SessionContextImplementor sessionContext, LoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, T> rootProjection) {
		return searchBackendContext.createSearchQueryBuilder(
				scopeModel.getElasticsearchIndexNames(),
				sessionContext,
				loadingContextBuilder, rootProjection
		);
	}
}
