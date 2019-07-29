/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

class Elasticsearch7SearchResultExtractor<H> implements ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> {

	protected static final JsonObjectAccessor HITS_ACCESSOR =
			JsonAccessor.root().property( "hits" ).asObject();

	private static final JsonAccessor<JsonArray> HITS_HITS_ACCESSOR =
			HITS_ACCESSOR.property( "hits" ).asArray();

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).property( "value" ).asLong();

	private static final JsonObjectAccessor AGGREGATIONS_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).asObject();

	private final ElasticsearchSearchQueryRequestContext requestContext;

	private final ElasticsearchSearchProjection<?, H> rootProjection;
	private final Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations;

	Elasticsearch7SearchResultExtractor(
			ElasticsearchSearchQueryRequestContext requestContext,
			ElasticsearchSearchProjection<?, H> rootProjection,
			Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations) {
		this.requestContext = requestContext;
		this.rootProjection = rootProjection;
		this.aggregations = aggregations;
	}

	@Override
	public ElasticsearchLoadableSearchResult<H> extract(JsonObject responseBody) {
		ElasticsearchSearchQueryExtractContext extractContext = requestContext.createExtractContext(
				responseBody
		);

		long hitCount = extractHitCount( responseBody );

		final List<Object> extractedHits = hitCount > 0 ?
				extractHits( extractContext ) : Collections.emptyList();

		Map<AggregationKey<?>, ?> extractedAggregations = aggregations.isEmpty() ?
				Collections.emptyMap() : extractAggregations( extractContext, responseBody );

		return new ElasticsearchLoadableSearchResult<>(
				extractContext,
				rootProjection,
				hitCount,
				extractedHits,
				extractedAggregations
		);
	}

	protected long extractHitCount(JsonObject responseBody) {
		return HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );
	}

	private List<Object> extractHits(ElasticsearchSearchQueryExtractContext extractContext) {
		JsonObject responseBody = extractContext.getResponseBody();
		ProjectionHitMapper<?, ?> hitMapper = extractContext.getProjectionHitMapper();
		JsonArray jsonHits = HITS_HITS_ACCESSOR.get( responseBody ).orElseGet( JsonArray::new );

		SearchProjectionExtractContext projectionExtractContext = extractContext.createProjectionExtractContext();
		List<Object> extractedData = new ArrayList<>( jsonHits.size() );

		for ( JsonElement hit : jsonHits ) {
			JsonObject hitObject = hit.getAsJsonObject();

			extractedData.add( rootProjection.extract(
					hitMapper, responseBody, hitObject,
					projectionExtractContext
			) );
		}

		return extractedData;
	}

	private Map<AggregationKey<?>, ?> extractAggregations(ElasticsearchSearchQueryExtractContext extractContext,
			JsonObject responseBody) {
		JsonObject jsonAggregations = AGGREGATIONS_ACCESSOR.get( responseBody ).orElseGet( JsonObject::new );

		Map<AggregationKey<?>, Object> extractedMap = new LinkedHashMap<>();

		for ( Map.Entry<AggregationKey<?>, ElasticsearchSearchAggregation<?>> entry : aggregations.entrySet() ) {
			AggregationKey<?> key = entry.getKey();
			ElasticsearchSearchAggregation<?> aggregation = entry.getValue();

			Object extracted = aggregation.extract( jsonAggregations.getAsJsonObject( key.getName() ), extractContext );
			extractedMap.put( key, extracted );
		}

		return extractedMap;
	}
}
