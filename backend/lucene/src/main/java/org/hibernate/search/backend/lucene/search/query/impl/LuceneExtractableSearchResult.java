/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.lucene.lowlevel.collector.impl.StoredFieldsCollector;
import org.hibernate.search.backend.lucene.search.aggregation.impl.AggregationExtractContext;
import org.hibernate.search.backend.lucene.search.aggregation.impl.LuceneSearchAggregation;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneResult;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.lucene.search.timeout.impl.TimeoutManager;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;

public class LuceneExtractableSearchResult<H> {

	private final LuceneSearchQueryExtractContext extractContext;
	private final LuceneSearchProjection<?, H> rootProjection;
	private final Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations;
	private final TimeoutManager timeoutManager;

	public LuceneExtractableSearchResult(LuceneSearchQueryExtractContext extractContext, LuceneSearchProjection<?, H> rootProjection,
			Map<AggregationKey<?>, LuceneSearchAggregation<?>> aggregations, TimeoutManager timeoutManager) {
		this.extractContext = extractContext;
		this.rootProjection = rootProjection;
		this.aggregations = aggregations;
		this.timeoutManager = timeoutManager;
	}

	public LuceneLoadableSearchResult<H> extract() throws IOException {
		TopDocs topDocs = extractContext.getTopDocs();
		if ( topDocs == null ) {
			return extract( 0, -1 );
		}

		return extract( 0, topDocs.scoreDocs.length - 1 );
	}

	public LuceneLoadableSearchResult<H> extract(int startIndex, int lastIndex) throws IOException {
		List<Object> extractedData = extractHits( startIndex, lastIndex );

		Map<AggregationKey<?>, ?> extractedAggregations = aggregations.isEmpty() ?
				Collections.emptyMap() : extractAggregations();

		return new LuceneLoadableSearchResult<>(
				extractContext, rootProjection,
				extractContext.getLuceneCollectors().getTotalHitCount(),
				extractedData,
				extractedAggregations,
				timeoutManager.getTookTime(),
				timeoutManager.isTimedOut()
		);
	}

	private List<Object> extractHits(int startIndex, int lastIndex) {
		ProjectionHitMapper<?, ?> projectionHitMapper = extractContext.getProjectionHitMapper();

		TopDocs topDocs = extractContext.getTopDocs();
		if ( topDocs == null ) {
			return Collections.emptyList();
		}

		List<Object> extractedData = new ArrayList<>( topDocs.scoreDocs.length );

		SearchProjectionExtractContext projectionExtractContext = extractContext.createProjectionExtractContext();

		StoredFieldsCollector storedFieldsCollector =
				projectionExtractContext.getCollector( StoredFieldsCollector.KEY );

		for ( int i = startIndex; i <= lastIndex; i++ ) {
			// Check for timeout every 16 elements.
			// Do this *before* the element, so that we don't fail after the last element.
			if ( i % 16 == 0 && timeoutManager.checkTimedOut() ) {
				break;
			}

			ScoreDoc hit = topDocs.scoreDocs[i];
			Document document = storedFieldsCollector == null ? null : storedFieldsCollector.getDocument( hit.doc );

			LuceneResult luceneResult = new LuceneResult( document, hit.doc, hit.score );

			extractedData.add( rootProjection.extract( projectionHitMapper, luceneResult, projectionExtractContext ) );
		}

		return extractedData;
	}

	private Map<AggregationKey<?>, ?> extractAggregations() throws IOException {
		AggregationExtractContext aggregationExtractContext = extractContext.createAggregationExtractContext();

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
}
