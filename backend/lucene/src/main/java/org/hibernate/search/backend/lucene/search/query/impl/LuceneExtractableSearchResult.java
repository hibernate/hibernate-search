/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollector;
import org.hibernate.search.backend.lucene.lowlevel.collector.impl.TopDocsDataCollectorExecutionContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectors;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.engine.backend.types.converter.runtime.FromDocumentValueConvertContext;
import org.hibernate.search.engine.backend.types.converter.runtime.spi.FromDocumentValueConvertContextImpl;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.timeout.spi.TimeoutManager;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class LuceneExtractableSearchResult<H> {

	private final LuceneSearchQueryRequestContext requestContext;
	private final FromDocumentValueConvertContext fromDocumentValueConvertContext;
	private final IndexSearcher indexSearcher;
	private final LuceneCollectors luceneCollectors;
	private final LuceneSearchProjection.Extractor<?, H> rootExtractor;
	private final Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private final TimeoutManager timeoutManager;

	public LuceneExtractableSearchResult(LuceneSearchQueryRequestContext requestContext,
			IndexSearcher indexSearcher,
			LuceneCollectors luceneCollectors,
			LuceneSearchProjection.Extractor<?, H> rootExtractor,
			Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations, TimeoutManager timeoutManager) {
		this.requestContext = requestContext;
		this.fromDocumentValueConvertContext = new FromDocumentValueConvertContextImpl( requestContext.getSessionContext() );
		this.indexSearcher = indexSearcher;
		this.luceneCollectors = luceneCollectors;
		this.rootExtractor = rootExtractor;
		this.aggregations = aggregations;
		this.timeoutManager = timeoutManager;
	}

	public LuceneLoadableSearchResult<H> extract() throws IOException {
		return extract( 0, Integer.MAX_VALUE );
	}

	public LuceneLoadableSearchResult<H> extract(int startInclusive, int endExclusive) throws IOException {
		TopDocs topDocs = luceneCollectors.getTopDocs();
		if ( topDocs == null ) {
			startInclusive = 0;
			endExclusive = 0;
		}
		else {
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			startInclusive = Math.min( startInclusive, scoreDocs.length );
			endExclusive = Math.min( endExclusive, scoreDocs.length );
		}

		ProjectionHitMapper<?> projectionHitMapper = requestContext.getLoadingContext().createProjectionHitMapper();
		List<Object> extractedData = extractHits( projectionHitMapper, startInclusive, endExclusive );

		Map<AggregationKey<?>, ?> extractedAggregations =
				aggregations.isEmpty() ? Collections.emptyMap() : extractAggregations();

		return new LuceneLoadableSearchResult<>(
				fromDocumentValueConvertContext, rootExtractor,
				luceneCollectors.getResultTotal(), luceneCollectors.getTopDocs(),
				extractedData, extractedAggregations, projectionHitMapper,
				timeoutManager.tookTime(),
				timeoutManager.isTimedOut(),
				timeoutManager
		);
	}

	public int hitSize() {
		TopDocs topDocs = luceneCollectors.getTopDocs();
		return ( topDocs == null ) ? 0 : topDocs.scoreDocs.length;
	}

	SearchResultTotal total() {
		return luceneCollectors.getResultTotal();
	}

	private List<Object> extractHits(ProjectionHitMapper<?> projectionHitMapper, int startInclusive,
			int endExclusive)
			throws IOException {
		TopDocs topDocs = luceneCollectors.getTopDocs();
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		TopDocsDataCollectorFactory collectorFactory = new TopDocsDataCollectorFactory( projectionHitMapper );

		return luceneCollectors.collectTopDocsData( collectorFactory, startInclusive, endExclusive );
	}

	private Map<AggregationKey<?>, ?> extractAggregations() throws IOException {
		AggregationExtractContext aggregationExtractContext = new AggregationExtractContext(
				indexSearcher.getIndexReader(),
				fromDocumentValueConvertContext,
				luceneCollectors.getCollectorsForAllMatchingDocs()
		);

		Map<AggregationKey<?>, Object> extractedMap = new LinkedHashMap<>();

		for ( Map.Entry<AggregationKey<?>, LuceneSearchAggregation<?>> entry : aggregations.entrySet() ) {
			// Check for timeout before every element.
			// Do this *before* the element, so that we don't fail after the last element.
			if ( timeoutManager.checkTimedOut() ) {
				break;
			}

			AggregationKey<?> key = entry.getKey();
			LuceneSearchAggregation<?> aggregation = entry.getValue();

			Object extracted = aggregation.extract( aggregationExtractContext );
			extractedMap.put( key, extracted );
		}

		return extractedMap;
	}

	private class TopDocsDataCollectorFactory implements TopDocsDataCollector.Factory<Object> {
		private final ProjectionHitMapper<?> projectionHitMapper;

		public TopDocsDataCollectorFactory(ProjectionHitMapper<?> projectionHitMapper) {
			this.projectionHitMapper = projectionHitMapper;
		}

		@Override
		public TopDocsDataCollector<Object> create(TopDocsDataCollectorExecutionContext context) throws IOException {
			ProjectionExtractContext projectionExtractContext =
					new ProjectionExtractContext( context, projectionHitMapper );
			return new TopDocsDataCollector<>( context, rootExtractor.values( projectionExtractContext ) );
		}
	}
}
