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
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.gson.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.gson.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;
import org.hibernate.search.engine.common.timing.Deadline;

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

	private static final JsonAccessor<String> HITS_TOTAL_RELATION_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).property( "relation" ).asString();

	private static final JsonObjectAccessor AGGREGATIONS_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).asObject();

	private static final JsonAccessor<Integer> TOOK_ACCESSOR =
			JsonAccessor.root().property( "took" ).asInteger();

	private static final JsonAccessor<Boolean> TIMED_OUT_ACCESSOR =
			JsonAccessor.root().property( "timed_out" ).asBoolean();

	private static final JsonAccessor<String> SCROLL_ID_ACCESSOR =
			JsonAccessor.root().property( "_scroll_id" ).asString();

	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR =
			JsonAccessor.root().property( "_source" ).asObject();

	private static final String HITS_TOTAL_RELATION_EXACT_VALUE = "eq";

	private final ElasticsearchSearchQueryRequestContext requestContext;

	private final ElasticsearchSearchProjection.Extractor<?, H> rootExtractor;
	private final Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations;

	Elasticsearch7SearchResultExtractor(
			ElasticsearchSearchQueryRequestContext requestContext,
			ElasticsearchSearchProjection.Extractor<?, H> rootExtractor,
			Map<AggregationKey<?>, ElasticsearchSearchAggregation<?>> aggregations) {
		this.requestContext = requestContext;
		this.rootExtractor = rootExtractor;
		this.aggregations = aggregations;
	}

	@Override
	public ElasticsearchLoadableSearchResult<H> extract(JsonObject responseBody,
			Deadline deadline) {
		ElasticsearchSearchQueryExtractContext extractContext = requestContext.createExtractContext(
				responseBody
		);

		Integer took = TOOK_ACCESSOR.get( responseBody ).get();
		boolean timedOut = TIMED_OUT_ACCESSOR.get( responseBody ).get();

		SearchResultTotal total = extractTotal( responseBody );
		if ( timedOut ) {
			// Elasticsearch doesn't return the correct relation in this case:
			// it tells us the count is exact, but it obviously isn't.
			total = SimpleSearchResultTotal.lowerBound( total.hitCountLowerBound() );
		}

		List<Object> extractedHits = ( total.isHitCountLowerBound() || total.hitCount() > 0 ) ?
				extractHits( extractContext ) : Collections.emptyList();

		Map<AggregationKey<?>, ?> extractedAggregations = aggregations.isEmpty() ?
				Collections.emptyMap() : extractAggregations( extractContext, responseBody );

		String scrollId = extractScrollId( responseBody );

		return new ElasticsearchLoadableSearchResult<>(
				extractContext,
				rootExtractor,
				total,
				extractedHits,
				extractedAggregations,
				took, timedOut, scrollId,
				deadline
		);
	}

	protected SearchResultTotal extractTotal(JsonObject responseBody) {
		Long hitsTotal = HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );
		Optional<String> hitsTotalRelation = HITS_TOTAL_RELATION_ACCESSOR.get( responseBody );
		boolean exact = hitsTotalRelation.isPresent()
				&& HITS_TOTAL_RELATION_EXACT_VALUE.equals( hitsTotalRelation.get() );
		return SimpleSearchResultTotal.of( hitsTotal, exact );
	}

	private List<Object> extractHits(ElasticsearchSearchQueryExtractContext extractContext) {
		JsonObject responseBody = extractContext.getResponseBody();
		ProjectionHitMapper<?> hitMapper = extractContext.getProjectionHitMapper();
		JsonArray jsonHits = HITS_HITS_ACCESSOR.get( responseBody ).orElseGet( JsonArray::new );

		ProjectionExtractContext projectionExtractContext = extractContext.createProjectionExtractContext();
		List<Object> extractedData = new ArrayList<>( jsonHits.size() );

		for ( JsonElement hit : jsonHits ) {
			JsonObject hitObject = hit.getAsJsonObject();
			JsonObject source = HIT_SOURCE_ACCESSOR.get( hitObject ).orElse( null );
			extractedData.add( rootExtractor.extract(
					hitMapper, hitObject, source, projectionExtractContext
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

			Object extracted = aggregation.extract( jsonAggregations.getAsJsonObject( key.name() ), extractContext );
			extractedMap.put( key, extracted );
		}

		return extractedMap;
	}

	protected String extractScrollId(JsonObject responseBody) {
		return SCROLL_ID_ACCESSOR.get( responseBody ).orElse( null );
	}
}
