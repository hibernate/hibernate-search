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
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

class LuceneSearchQueryBuilderFactory
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
	public <O> SearchQueryBuilder<O, LuceneSearchQueryElementCollector> asObject(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, O> projectionHitMapper) {
		return createSearchQueryBuilder( sessionContext, projectionHitMapper, LuceneObjectProjection.get() );
	}

	@Override
	public <T> SearchQueryBuilder<T, LuceneSearchQueryElementCollector> asReference(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper) {
		return createSearchQueryBuilder( sessionContext, projectionHitMapper, LuceneReferenceProjection.get() );
	}

	@Override
	public <T> SearchQueryBuilder<T, LuceneSearchQueryElementCollector> asProjection(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper,
			SearchProjection<T> projection) {
		return createSearchQueryBuilder( sessionContext, projectionHitMapper,
				searchProjectionFactory.toImplementation( projection ) );
	}

	@Override
	public SearchQueryBuilder<List<?>, LuceneSearchQueryElementCollector> asProjections(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper,
			SearchProjection<?>... projections) {
		return createSearchQueryBuilder( sessionContext, projectionHitMapper, createRootProjection( projections ) );
	}

	private LuceneSearchProjection<?, List<?>> createRootProjection(SearchProjection<?>[] projections) {
		List<LuceneSearchProjection<?, ?>> children = new ArrayList<>( projections.length );

		for ( SearchProjection<?> projection : projections ) {
			children.add( searchProjectionFactory.toImplementation( projection ) );
		}

		return new LuceneCompositeListProjection<>( Function.identity(), children );
	}

	private <T> LuceneSearchQueryBuilder<T> createSearchQueryBuilder(
			SessionContextImplementor sessionContext, ProjectionHitMapper<?, ?> projectionHitMapper,
			LuceneSearchProjection<?, T> rootProjection) {
		return searchBackendContext.createSearchQueryBuilder(
				scopeModel, sessionContext, projectionHitMapper, rootProjection
		);
	}
}
