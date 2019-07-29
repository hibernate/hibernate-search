/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import static org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection.transformUnsafe;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionTransformContext;
import org.hibernate.search.backend.elasticsearch.search.query.ElasticsearchSearchResult;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;

/**
 * A search result from the backend that offers a method to load data from the mapper.
 * <p>
 * Allows to run loading in the user thread, and not in the backend HTTP request threads.
 * <p>
 * <strong>WARNING:</strong> loading should only be triggered once.
 * <p>
 * <strong>WARNING:</strong> this class is not thread-safe.
 *
 * @param <H> The type of hits in the search result.
 */
public class ElasticsearchLoadableSearchResult<H> {
	private final ElasticsearchSearchQueryExtractContext extractContext;
	private final ElasticsearchSearchProjection<?, H> rootProjection;

	private final long hitCount;
	private List<Object> extractedHits;
	private final Map<AggregationKey<?>, ?> extractedAggregations;

	ElasticsearchLoadableSearchResult(ElasticsearchSearchQueryExtractContext extractContext,
			ElasticsearchSearchProjection<?, H> rootProjection,
			long hitCount,
			List<Object> extractedHits,
			Map<AggregationKey<?>, ?> extractedAggregations) {
		this.extractContext = extractContext;
		this.rootProjection = rootProjection;
		this.hitCount = hitCount;
		this.extractedHits = extractedHits;
		this.extractedAggregations = extractedAggregations;
	}

	ElasticsearchSearchResult<H> loadBlocking() {
		SearchProjectionTransformContext transformContext = extractContext.createProjectionTransformContext();

		LoadingResult<?> loadingResult = extractContext.getProjectionHitMapper().loadBlocking();

		int readIndex = 0;
		int writeIndex = 0;
		for ( ; readIndex < extractedHits.size(); ++readIndex ) {
			transformContext.reset();
			H transformed = transformUnsafe(
					rootProjection, loadingResult, extractedHits.get( readIndex ), transformContext
			);

			if ( transformContext.hasFailedLoad() ) {
				// Skip the hit
				continue;
			}

			extractedHits.set( writeIndex, transformed );
			++writeIndex;
		}

		if ( writeIndex < readIndex ) {
			// Some hits were skipped; adjust the list size.
			extractedHits.subList( writeIndex, readIndex ).clear();
		}

		// The cast is safe, since all elements extend H and we make the list unmodifiable
		@SuppressWarnings("unchecked")
		List<H> loadedHits = Collections.unmodifiableList( (List<? extends H>) extractedHits );

		// Make sure that if someone uses this object incorrectly, it will always fail, and will fail early.
		extractedHits = null;

		return new ElasticsearchSearchResultImpl<>( hitCount, loadedHits, extractedAggregations );
	}
}
