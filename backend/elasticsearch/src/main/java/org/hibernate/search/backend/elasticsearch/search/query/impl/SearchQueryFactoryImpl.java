/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchQueryElementCollector;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchTargetModel;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.SearchProjection;
import org.hibernate.search.engine.search.query.spi.DocumentReferenceHitCollector;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

class SearchQueryFactoryImpl
		implements SearchQueryFactory<ElasticsearchSearchQueryElementCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;

	private final ElasticsearchSearchTargetModel searchTargetModel;

	private final ElasticsearchSearchProjectionFactoryImpl searchProjectionFactory;

	SearchQueryFactoryImpl(SearchBackendContext searchBackendContext, ElasticsearchSearchTargetModel searchTargetModel,
			ElasticsearchSearchProjectionFactoryImpl searchProjectionFactory) {
		this.searchBackendContext = searchBackendContext;
		this.searchTargetModel = searchTargetModel;
		this.searchProjectionFactory = searchProjectionFactory;
	}

	@Override
	public <O> SearchQueryBuilder<O, ElasticsearchSearchQueryElementCollector> asObjects(
			SessionContext sessionContext, HitAggregator<LoadingHitCollector, List<O>> hitAggregator) {
		return createSearchQueryBuilder(
				sessionContext, searchBackendContext.getObjectHitExtractor(), hitAggregator
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> asReferences(
			SessionContext sessionContext, HitAggregator<DocumentReferenceHitCollector, List<T>> hitAggregator) {
		return createSearchQueryBuilder(
				sessionContext, searchBackendContext.getDocumentReferenceHitExtractor(), hitAggregator
		);
	}

	@Override
	public <T> SearchQueryBuilder<T, ElasticsearchSearchQueryElementCollector> asProjections(
			SessionContext sessionContext, HitAggregator<ProjectionHitCollector, List<T>> hitAggregator,
			SearchProjection<?>... projections) {
		BitSet projectionFound = new BitSet( projections.length );

		HitExtractor<? super ProjectionHitCollector> hitExtractor;
		Set<ElasticsearchIndexModel> indexModels = searchTargetModel.getIndexModels();
		if ( indexModels.size() == 1 ) {
			ElasticsearchIndexModel indexModel = indexModels.iterator().next();
			hitExtractor = createProjectionHitExtractor( indexModel, projections, projectionFound );
		}
		else {
			// Use LinkedHashMap to ensure stable order when generating requests
			Map<String, HitExtractor<? super ProjectionHitCollector>> extractorByElasticsearchIndexName = new LinkedHashMap<>();
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				HitExtractor<? super ProjectionHitCollector> indexHitExtractor =
						createProjectionHitExtractor( indexModel, projections, projectionFound );
				extractorByElasticsearchIndexName.put( indexModel.getElasticsearchIndexName().original, indexHitExtractor );
			}
			hitExtractor = new IndexSensitiveHitExtractor<>( extractorByElasticsearchIndexName );
		}
		if ( projectionFound.cardinality() < projections.length ) {
			projectionFound.flip( 0, projections.length );
			List<SearchProjection<?>> unknownProjections = projectionFound.stream()
					.mapToObj( i -> projections[i] )
					.collect( Collectors.toList() );
			throw log.unknownProjectionForSearch( unknownProjections, searchTargetModel.getIndexesEventContext() );
		}

		return createSearchQueryBuilder( sessionContext, hitExtractor, hitAggregator );
	}

	private HitExtractor<? super ProjectionHitCollector> createProjectionHitExtractor(
			ElasticsearchIndexModel indexModel, SearchProjection<?>[] projections, BitSet projectionFound) {
		List<HitExtractor<? super ProjectionHitCollector>> extractors = new ArrayList<>( projections.length );
		for ( int i = 0; i < projections.length; ++i ) {
			ElasticsearchSearchProjection<?> projection = searchProjectionFactory.toImplementation( projections[i] );

			Optional<HitExtractor<? super ProjectionHitCollector>> hitExtractorOptional =
					projection.getHitExtractor( searchBackendContext, indexModel );
			if ( hitExtractorOptional.isPresent() ) {
				projectionFound.set( i );
				extractors.add( hitExtractorOptional.get() );
			}
			else {
				extractors.add( NullProjectionHitExtractor.get() );
			}
		}
		return new CompositeHitExtractor<>( extractors );
	}

	private <C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			SessionContext sessionContext, HitExtractor<? super C> hitExtractor, HitAggregator<C, List<T>> hitAggregator) {
		return searchBackendContext.createSearchQueryBuilder(
				searchTargetModel.getElasticsearchIndexNames(),
				sessionContext,
				hitExtractor, hitAggregator
		);
	}
}
