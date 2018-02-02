/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexSchemaFieldNode;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.impl.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.predicate.impl.ElasticsearchSearchPredicateCollector;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.HitCollector;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;
import org.hibernate.search.util.spi.LoggerFactory;

class SearchQueryFactoryImpl implements SearchQueryFactory<ElasticsearchSearchPredicateCollector> {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchBackend backend;

	private final Set<ElasticsearchIndexModel> indexModels;

	private final Set<String> indexNames;

	SearchQueryFactoryImpl(ElasticsearchBackend backend, Set<ElasticsearchIndexModel> indexModels,
			Set<String> indexNames) {
		this.backend = backend;
		this.indexModels = indexModels;
		this.indexNames = indexNames;
	}

	@Override
	public <R, O> SearchQueryBuilder<O, ElasticsearchSearchPredicateCollector> asObjects(
			SessionContext sessionContext, Function<DocumentReference, R> documentReferenceTransformer,
			HitAggregator<LoadingHitCollector<R>, List<O>> hitAggregator) {
		HitExtractor<LoadingHitCollector<? super R>> hitExtractor =
				new ObjectHitExtractor<>( documentReferenceTransformer );
		return createSearchQueryBuilder( sessionContext, hitExtractor, hitAggregator );
	}

	@Override
	public <R, T> SearchQueryBuilder<T, ElasticsearchSearchPredicateCollector> asReferences(
			SessionContext sessionContext, Function<DocumentReference, R> documentReferenceTransformer,
			HitAggregator<HitCollector<R>, List<T>> hitAggregator) {
		HitExtractor<HitCollector<? super R>> hitExtractor =
				new DocumentReferenceHitExtractor<>( documentReferenceTransformer );
		return createSearchQueryBuilder( sessionContext, hitExtractor, hitAggregator );
	}

	@Override
	public <R, T> SearchQueryBuilder<T, ElasticsearchSearchPredicateCollector> asProjections(
			SessionContext sessionContext, Function<DocumentReference, R> documentReferenceTransformer,
			HitAggregator<ProjectionHitCollector<R>, List<T>> hitAggregator, String... projections) {
		BitSet projectionFound = new BitSet( projections.length );

		HitExtractor<? super ProjectionHitCollector<R>> hitExtractor;
		if ( indexModels.size() == 1 ) {
			ElasticsearchIndexModel indexModel = indexModels.iterator().next();
			hitExtractor = createProjectionHitExtractor(
					documentReferenceTransformer, indexModel, projections, projectionFound );
		}
		else {
			// Use LinkedHashMap to ensure stable order when generating requests
			Map<String, HitExtractor<? super ProjectionHitCollector<R>>> extractorByIndex = new LinkedHashMap<>();
			for ( ElasticsearchIndexModel indexModel : indexModels ) {
				HitExtractor<? super ProjectionHitCollector<R>> indexHitExtractor =
						createProjectionHitExtractor( documentReferenceTransformer, indexModel, projections, projectionFound );
				extractorByIndex.put( indexModel.getIndexName(), indexHitExtractor );
			}
			hitExtractor = new IndexSensitiveHitExtractor<>( extractorByIndex );
		}
		if ( projectionFound.cardinality() < projections.length ) {
			projectionFound.flip( 0, projectionFound.length() );
			List<String> unknownProjections = projectionFound.stream()
					.mapToObj( i -> projections[i] )
					.collect( Collectors.toList() );
			throw log.unknownProjectionForSearch( unknownProjections, indexNames );
		}

		return createSearchQueryBuilder( sessionContext, hitExtractor, hitAggregator );
	}

	private <R> HitExtractor<? super ProjectionHitCollector<R>> createProjectionHitExtractor(
			Function<DocumentReference, R> documentReferenceTransformer, ElasticsearchIndexModel indexModel,
			String[] projections, BitSet projectionFound) {
		List<HitExtractor<? super ProjectionHitCollector<R>>> extractors = new ArrayList<>( projections.length );
		for ( int i = 0; i < projections.length; ++i ) {
			String projection = projections[i];
			switch ( projection ) {
				case ProjectionConstants.OBJECT:
					projectionFound.set( i );
					extractors.add( new ObjectHitExtractor<>( documentReferenceTransformer ) );
					break;
				case ProjectionConstants.REFERENCE:
					projectionFound.set( i );
					extractors.add( new DocumentReferenceHitExtractor<>( documentReferenceTransformer ) );
					break;
				case ProjectionConstants.DOCUMENT_REFERENCE:
					projectionFound.set( i );
					extractors.add( new DocumentReferenceHitExtractor<>( Function.identity() ) );
					break;
				default:
					ElasticsearchIndexSchemaFieldNode node = indexModel.getFieldNode( projection );
					if ( node != null ) {
						projectionFound.set( i );
						extractors.add( new SourceHitExtractor( projection, node.getFormatter() ) );
					}
					else {
						// Make sure that the result list will have the correct indices and size
						extractors.add( NullHitExtractor.get() );
					}
					break;
			}
		}
		return new CompositeHitExtractor<>( extractors );
	}

	private <C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			SessionContext sessionContext, HitExtractor<? super C> hitExtractor, HitAggregator<C, List<T>> hitAggregator) {
		return new SearchQueryBuilderImpl<>(
				backend.getQueryOrchestrator(),
				backend.getWorkFactory(),
				indexNames,
				sessionContext,
				hitExtractor, hitAggregator
		);
	}
}
