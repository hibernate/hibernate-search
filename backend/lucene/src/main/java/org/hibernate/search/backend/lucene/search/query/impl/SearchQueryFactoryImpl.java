/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchQueryElementCollector;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchTargetModel;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.engine.search.query.spi.DocumentReferenceHitCollector;
import org.hibernate.search.engine.search.query.spi.HitAggregator;
import org.hibernate.search.engine.search.query.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.query.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.query.spi.SearchQueryBuilder;
import org.hibernate.search.engine.search.query.spi.SearchQueryFactory;
import org.hibernate.search.util.impl.common.LoggerFactory;

class SearchQueryFactoryImpl
		implements SearchQueryFactory<LuceneSearchQueryElementCollector> {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final SearchBackendContext searchBackendContext;

	private final LuceneSearchTargetModel searchTargetModel;

	SearchQueryFactoryImpl(SearchBackendContext searchBackendContext, LuceneSearchTargetModel searchTargetModel) {
		this.searchBackendContext = searchBackendContext;
		this.searchTargetModel = searchTargetModel;
	}

	@Override
	public <O> SearchQueryBuilder<O, LuceneSearchQueryElementCollector> asObjects(
			SessionContext sessionContext, HitAggregator<LoadingHitCollector, List<O>> hitAggregator) {
		return createSearchQueryBuilder( sessionContext, ObjectHitExtractor.get(), hitAggregator );
	}

	@Override
	public <T> SearchQueryBuilder<T, LuceneSearchQueryElementCollector> asReferences(
			SessionContext sessionContext, HitAggregator<DocumentReferenceHitCollector, List<T>> hitAggregator) {
		return createSearchQueryBuilder( sessionContext, DocumentReferenceHitExtractor.get(), hitAggregator );
	}

	@Override
	public <T> SearchQueryBuilder<T, LuceneSearchQueryElementCollector> asProjections(
			SessionContext sessionContext, HitAggregator<ProjectionHitCollector, List<T>> hitAggregator,
			String... projections) {
		BitSet projectionFound = new BitSet( projections.length );

		HitExtractor<? super ProjectionHitCollector> hitExtractor;
		Set<LuceneIndexModel> indexModels = searchTargetModel.getIndexModels();
		if ( indexModels.size() == 1 ) {
			LuceneIndexModel indexModel = indexModels.iterator().next();
			hitExtractor = createProjectionHitExtractor( indexModel, projections, projectionFound );
		}
		else {
			// Use LinkedHashMap to ensure stable order when generating requests
			Map<String, HitExtractor<? super ProjectionHitCollector>> extractorByIndex = new LinkedHashMap<>();
			for ( LuceneIndexModel indexModel : indexModels ) {
				HitExtractor<? super ProjectionHitCollector> indexHitExtractor =
						createProjectionHitExtractor( indexModel, projections, projectionFound );
				extractorByIndex.put( indexModel.getIndexName(), indexHitExtractor );
			}
			hitExtractor = new IndexSensitiveHitExtractor<>( extractorByIndex );
		}
		if ( projectionFound.cardinality() < projections.length ) {
			projectionFound.flip( 0, projections.length );
			List<String> unknownProjections = projectionFound.stream()
					.mapToObj( i -> projections[i] )
					.collect( Collectors.toList() );
			throw log.unknownProjectionForSearch( unknownProjections, searchTargetModel.getIndexNames() );
		}

		return createSearchQueryBuilder( sessionContext, hitExtractor, hitAggregator );
	}

	private HitExtractor<? super ProjectionHitCollector> createProjectionHitExtractor(
			LuceneIndexModel indexModel, String[] projections, BitSet projectionFound) {
		List<HitExtractor<? super ProjectionHitCollector>> extractors = new ArrayList<>( projections.length );
		for ( int i = 0; i < projections.length; ++i ) {
			String projection = projections[i];
			switch ( projection ) {
				case ProjectionConstants.OBJECT:
					projectionFound.set( i );
					extractors.add( ObjectHitExtractor.get() );
					break;
				case ProjectionConstants.REFERENCE:
					projectionFound.set( i );
					extractors.add( DocumentReferenceHitExtractor.get() );
					break;
				case ProjectionConstants.DOCUMENT_REFERENCE:
					projectionFound.set( i );
					extractors.add( DocumentReferenceProjectionHitExtractor.get() );
					break;
				default:
					LuceneIndexSchemaFieldNode<?> schemaNode = indexModel.getFieldNode( projection );
					if ( schemaNode != null ) {
						projectionFound.set( i );
						extractors.add( new FieldProjectionHitExtractor( projection, schemaNode.getCodec() ) );
					}
					else {
						// Make sure that the result list will have the correct indices and size
						extractors.add( NullProjectionHitExtractor.get() );
					}
					break;
			}
		}
		return new CompositeHitExtractor<>( extractors );
	}

	private <C, T> SearchQueryBuilderImpl<C, T> createSearchQueryBuilder(
			SessionContext sessionContext, HitExtractor<? super C> hitExtractor, HitAggregator<C, List<T>> hitAggregator) {
		return searchBackendContext.createSearchQueryBuilder(
				searchTargetModel, sessionContext, hitExtractor, hitAggregator
		);
	}
}
