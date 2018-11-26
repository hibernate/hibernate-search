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
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchCompositeListProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjectionBuilderFactory;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchObjectProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchReferenceProjection;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

class SearchQueryBuilderFactoryImpl
		implements SearchQueryBuilderFactory<ElasticsearchSearchQueryElementCollector> {

	private final SearchBackendContext searchBackendContext;

	private final ElasticsearchSearchTargetModel searchTargetModel;

	private final ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory;

	SearchQueryBuilderFactoryImpl(SearchBackendContext searchBackendContext, ElasticsearchSearchTargetModel searchTargetModel,
			ElasticsearchSearchProjectionBuilderFactory searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.searchTargetModel = searchTargetModel;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <O> SearchQueryBuilder<O, ElasticsearchSearchQueryElementCollector> asObject(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, O> projectionHitMapper) {
		return createSearchQueryBuilder(
				sessionContext, projectionHitMapper,
				new ElasticsearchObjectProjection<>( searchBackendContext.getDocumentReferenceExtractorHelper() )
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> asReference(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper) {
		return createSearchQueryBuilder(
				sessionContext, projectionHitMapper,
				new ElasticsearchReferenceProjection<>( searchBackendContext.getDocumentReferenceExtractorHelper() )
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> asProjection(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper,
			SearchProjection<T> projection) {
		return createSearchQueryBuilder( sessionContext, projectionHitMapper,
				searchProjectionFactory.toImplementation( projection ) );
	}

	@Override
	public SearchQueryBuilder<List<?>, ElasticsearchSearchQueryElementCollector> asProjections(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper,
			SearchProjection<?>... projections) {
		return createSearchQueryBuilder( sessionContext, projectionHitMapper, createRootProjection( projections ) );
	}

	private ElasticsearchSearchProjection<?, List<?>> createRootProjection(SearchProjection<?>[] projections) {
		List<ElasticsearchSearchProjection<?, ?>> children = new ArrayList<>( projections.length );

		for ( int i = 0; i < projections.length; ++i ) {
			children.add( searchProjectionFactory.toImplementation( projections[i] ) );
		}

		return new ElasticsearchCompositeListProjection<>( Function.identity(), children );
	}

	private <T> SearchQueryBuilderImpl<T> createSearchQueryBuilder(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper,
			ElasticsearchSearchProjection<?, T> rootProjection) {
		return searchBackendContext.createSearchQueryBuilder(
				searchTargetModel.getElasticsearchIndexNames(),
				sessionContext,
				projectionHitMapper, rootProjection
		);
	}
}
