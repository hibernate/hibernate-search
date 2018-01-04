/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchFieldModel;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.elasticsearch.search.dsl.impl.ElasticsearchSearchTargetContext;
import org.hibernate.search.engine.search.dsl.spi.SearchQueryWrappingDefinitionResultContextImpl;
import org.hibernate.search.engine.common.spi.SessionContext;
import org.hibernate.search.engine.search.DocumentReference;
import org.hibernate.search.engine.search.ObjectLoader;
import org.hibernate.search.engine.search.ProjectionConstants;
import org.hibernate.search.engine.search.SearchQuery;
import org.hibernate.search.engine.search.spi.HitAggregator;
import org.hibernate.search.engine.search.spi.HitCollector;
import org.hibernate.search.engine.search.spi.LoadingHitCollector;
import org.hibernate.search.engine.search.spi.ObjectHitAggregator;
import org.hibernate.search.engine.search.spi.ProjectionHitAggregator;
import org.hibernate.search.engine.search.spi.ProjectionHitCollector;
import org.hibernate.search.engine.search.spi.SearchQueryResultDefinitionContext;
import org.hibernate.search.engine.search.spi.SearchQueryWrappingDefinitionResultContext;
import org.hibernate.search.engine.search.spi.SimpleHitAggregator;
import org.hibernate.search.util.spi.LoggerFactory;

public class SearchQueryResultDefinitionContextImpl<R, O> implements SearchQueryResultDefinitionContext<R, O> {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ElasticsearchSearchTargetContext targetContext;

	private final SessionContext sessionContext;

	private final Function<DocumentReference, R> documentReferenceTransformer;

	private final ObjectLoader<R, O> objectLoader;

	public SearchQueryResultDefinitionContextImpl(ElasticsearchSearchTargetContext targetContext,
			SessionContext sessionContext,
			Function<DocumentReference, R> documentReferenceTransformer,
			ObjectLoader<R, O> objectLoader) {
		this.targetContext = targetContext;
		this.sessionContext = sessionContext;
		this.documentReferenceTransformer = documentReferenceTransformer;
		this.objectLoader = objectLoader;
	}

	@Override
	public SearchQueryWrappingDefinitionResultContext<SearchQuery<O>> asObjects() {
		HitExtractor<LoadingHitCollector<? super R>> hitExtractor =
				new ObjectHitExtractor<>( documentReferenceTransformer );
		HitAggregator<LoadingHitCollector<R>, List<O>> hitAggregator =
				new ObjectHitAggregator<>( objectLoader );
		SearchQueryBuilderImpl<?, O> builder = createSearchQueryBuilder( hitExtractor, hitAggregator );
		return new SearchQueryWrappingDefinitionResultContextImpl<>( targetContext, builder, Function.identity() );
	}

	@Override
	public <T> SearchQueryWrappingDefinitionResultContext<SearchQuery<T>> asReferences(Function<R, T> hitTransformer) {
		HitExtractor<HitCollector<? super R>> hitExtractor =
				new DocumentReferenceHitExtractor<>( documentReferenceTransformer );
		HitAggregator<HitCollector<R>, List<T>> hitAggregator =
				new SimpleHitAggregator<>( hitTransformer );
		SearchQueryBuilderImpl<?, T> builder = createSearchQueryBuilder( hitExtractor, hitAggregator );
		return new SearchQueryWrappingDefinitionResultContextImpl<>( targetContext, builder, Function.identity() );
	}

	@Override
	public <T> SearchQueryWrappingDefinitionResultContext<SearchQuery<T>> asProjections(
			Function<List<?>, T> hitTransformer, String... projections) {
		BitSet projectionFound = new BitSet( projections.length );

		HitExtractor<? super ProjectionHitCollector<R>> hitExtractor;
		if ( targetContext.getIndexModels().size() == 1 ) {
			ElasticsearchIndexModel indexModel = targetContext.getIndexModels().iterator().next();
			hitExtractor = createProjectionHitExtractor( indexModel, projections, projectionFound );
		}
		else {
			// Use LinkedHashMap to ensure stable order when generating requests
			Map<String, HitExtractor<? super ProjectionHitCollector<R>>> extractorByIndex = new LinkedHashMap<>();
			for ( ElasticsearchIndexModel indexModel : targetContext.getIndexModels() ) {
				HitExtractor<? super ProjectionHitCollector<R>> indexHitExtractor =
						createProjectionHitExtractor( indexModel, projections, projectionFound );
				extractorByIndex.put( indexModel.getIndexName(), indexHitExtractor );
			}
			hitExtractor = new IndexSensitiveHitExtractor<>( extractorByIndex );
		}
		if ( projectionFound.cardinality() < projections.length ) {
			projectionFound.flip( 0, projectionFound.length() );
			List<String> unknownProjections = projectionFound.stream()
					.mapToObj( i -> projections[i] )
					.collect( Collectors.toList() );
			throw log.unknownProjectionForSearch( unknownProjections, targetContext.getIndexNames() );
		}

		int expectedLoadPerHit = (int) Arrays.stream( projections )
				.filter( Predicate.isEqual( ProjectionConstants.OBJECT ) )
				.count();
		HitAggregator<ProjectionHitCollector<R>, List<T>> hitAggregator =
				new ProjectionHitAggregator<>( objectLoader, hitTransformer, projections.length, expectedLoadPerHit );

		SearchQueryBuilderImpl<?, T> builder = createSearchQueryBuilder( hitExtractor, hitAggregator );
		return new SearchQueryWrappingDefinitionResultContextImpl<>( targetContext, builder, Function.identity() );
	}

	private HitExtractor<? super ProjectionHitCollector<R>> createProjectionHitExtractor(
			ElasticsearchIndexModel indexModel, String[] projections, BitSet projectionFound) {
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
					ElasticsearchFieldModel fieldModel = indexModel.getFieldModel( projection );
					if ( fieldModel != null ) {
						projectionFound.set( i );
						extractors.add( new SourceHitExtractor( projection, fieldModel.getFormatter() ) );
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
			HitExtractor<? super C> hitExtractor, HitAggregator<C, List<T>> hitAggregator) {
		return new SearchQueryBuilderImpl<>(
				targetContext.getQueryOrchestrator(),
				targetContext.getWorkFactory(),
				targetContext.getIndexNames(),
				sessionContext,
				hitExtractor, hitAggregator
		);
	}
}
