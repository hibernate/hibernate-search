/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.backend.lucene.search.extraction.impl.CompositeHitExtractor;
import org.hibernate.search.backend.lucene.search.extraction.impl.ReferenceHitExtractor;
import org.hibernate.search.backend.lucene.search.extraction.impl.HitExtractor;
import org.hibernate.search.backend.lucene.search.extraction.impl.ObjectHitExtractor;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjectionBuilderFactoryImpl;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.ReferenceHitCollector;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilderFactory;

class SearchQueryBuilderFactoryImpl
		implements SearchQueryBuilderFactory<LuceneSearchQueryElementCollector> {

	private final SearchBackendContext searchBackendContext;

	private final LuceneSearchTargetModel searchTargetModel;

	private final LuceneSearchProjectionBuilderFactoryImpl searchProjectionFactory;

	SearchQueryBuilderFactoryImpl(SearchBackendContext searchBackendContext,
			LuceneSearchTargetModel searchTargetModel,
			LuceneSearchProjectionBuilderFactoryImpl searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.searchTargetModel = searchTargetModel;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <O> SearchQueryBuilder<O, LuceneSearchQueryElementCollector> asObject(
			SessionContextImplementor sessionContext, HitAggregator<LoadingHitCollector, List<O>> hitAggregator) {
		return createSearchQueryBuilder( sessionContext, ObjectHitExtractor.get(), hitAggregator );
	}

	@Override
	public <T> SearchQueryBuilder<T, LuceneSearchQueryElementCollector> asReference(
			SessionContextImplementor sessionContext, HitAggregator<ReferenceHitCollector, List<T>> hitAggregator) {
		return createSearchQueryBuilder( sessionContext, ReferenceHitExtractor.get(), hitAggregator );
	}

	@Override
	public <T> SearchQueryBuilder<T, LuceneSearchQueryElementCollector> asProjections(
			SessionContextImplementor sessionContext, HitAggregator<ProjectionHitCollector, List<T>> hitAggregator,
			SearchProjection<?>... projections) {
		HitExtractor<? super ProjectionHitCollector> hitExtractor = createProjectionHitExtractor( projections );

		return createSearchQueryBuilder( sessionContext, hitExtractor, hitAggregator );
	}

	private HitExtractor<? super ProjectionHitCollector> createProjectionHitExtractor(
			SearchProjection<?>[] projections) {
		if ( projections.length == 1 ) {
			return searchProjectionFactory.toImplementation( projections[0] );
		}

		List<HitExtractor<? super ProjectionHitCollector>> extractors = new ArrayList<>( projections.length );
		for ( int i = 0; i < projections.length; ++i ) {
			extractors.add( searchProjectionFactory.toImplementation( projections[i] ) );
		}

		return new CompositeHitExtractor<>( extractors );
	}

	private <C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			SessionContextImplementor sessionContext, HitExtractor<? super C> hitExtractor, HitAggregator<C, List<T>> hitAggregator) {
		return searchBackendContext.createSearchQueryBuilder(
				searchTargetModel, sessionContext, hitExtractor, hitAggregator
		);
	}
}
