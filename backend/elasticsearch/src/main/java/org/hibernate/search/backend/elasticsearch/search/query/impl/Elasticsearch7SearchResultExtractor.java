package org.hibernate.search.backend.elasticsearch.search.query.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hibernate.search.backend.elasticsearch.json.impl.JsonAccessor;
import org.hibernate.search.backend.elasticsearch.json.impl.JsonObjectAccessor;
import org.hibernate.search.backend.elasticsearch.search.aggregation.impl.ElasticsearchSearchAggregation;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ProjectionExtractContext;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchSearchResultExtractor;
import org.hibernate.search.engine.common.timing.Deadline;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.SearchResultTotal;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResultTotal;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

class Elasticsearch7SearchResultExtractor<H>
		implements ElasticsearchSearchResultExtractor<ElasticsearchLoadableSearchResult<H>> {

	protected static final JsonObjectAccessor HITS_ACCESSOR =
			JsonAccessor.root().property( "hits" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<ArrayNode> HITS_HITS_ACCESSOR =
			HITS_ACCESSOR.property( "hits" ).asArray();

	private static final JsonAccessor<Long> HITS_TOTAL_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).unwrapSingletonArrays().property( "value" ).unwrapSingletonArrays().asLong();

	private static final JsonAccessor<String> HITS_TOTAL_RELATION_ACCESSOR =
			HITS_ACCESSOR.property( "total" ).unwrapSingletonArrays().property( "relation" ).unwrapSingletonArrays().asString();

	private static final JsonObjectAccessor AGGREGATIONS_ACCESSOR =
			JsonAccessor.root().property( "aggregations" ).unwrapSingletonArrays().asObject();

	private static final JsonAccessor<Integer> TOOK_ACCESSOR =
			JsonAccessor.root().property( "took" ).unwrapSingletonArrays().asInteger();

	private static final JsonAccessor<Boolean> TIMED_OUT_ACCESSOR =
			JsonAccessor.root().property( "timed_out" ).unwrapSingletonArrays().asBoolean();

	private static final JsonAccessor<String> SCROLL_ID_ACCESSOR =
			JsonAccessor.root().property( "_scroll_id" ).unwrapSingletonArrays().asString();

	private static final JsonObjectAccessor HIT_SOURCE_ACCESSOR =
			JsonAccessor.root().property( "_source" ).unwrapSingletonArrays().asObject();

	private static final String HITS_TOTAL_RELATION_EXACT_VALUE = "eq";

	private final ElasticsearchSearchQueryRequestContext requestContext;

	private final ElasticsearchSearchProjection.Extractor<?, H> rootExtractor;
	private final List<ElasticsearchSearchAggregation.Extractor<?>> aggregations;

	Elasticsearch7SearchResultExtractor(
			ElasticsearchSearchQueryRequestContext requestContext,
			ElasticsearchSearchProjection.Extractor<?, H> rootExtractor,
			List<ElasticsearchSearchAggregation.Extractor<?>> aggregations) {
		this.requestContext = requestContext;
		this.rootExtractor = rootExtractor;
		this.aggregations = aggregations;
	}

	@Override
	public ElasticsearchLoadableSearchResult<H> extract(ObjectNode responseBody, Deadline deadline) {
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

		List<Object> extractedHits = ( total.isHitCountLowerBound() || total.hitCount() > 0 )
				? extractHits( extractContext )
				: Collections.emptyList();

		Map<AggregationKey<?>, ?> extractedAggregations =
				aggregations.isEmpty() ? Collections.emptyMap() : extractAggregations( extractContext, responseBody );

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

	protected SearchResultTotal extractTotal(ObjectNode responseBody) {
		Long hitsTotal = HITS_TOTAL_ACCESSOR.get( responseBody ).orElse( 0L );
		Optional<String> hitsTotalRelation = HITS_TOTAL_RELATION_ACCESSOR.get( responseBody );
		boolean exact = hitsTotalRelation.isPresent()
				&& HITS_TOTAL_RELATION_EXACT_VALUE.equals( hitsTotalRelation.get() );
		return SimpleSearchResultTotal.of( hitsTotal, exact );
	}

	private List<Object> extractHits(ElasticsearchSearchQueryExtractContext extractContext) {
		ObjectNode responseBody = extractContext.getResponseBody();
		ProjectionHitMapper<?> hitMapper = extractContext.getProjectionHitMapper();
		ArrayNode jsonHits = HITS_HITS_ACCESSOR.get( responseBody ).orElseGet( JsonNodeFactory.instance::arrayNode );

		ProjectionExtractContext projectionExtractContext = extractContext.createProjectionExtractContext();
		List<Object> extractedData = new ArrayList<>( jsonHits.size() );

		for ( JsonNode hit : jsonHits ) {
			ObjectNode hitObject = ( (ObjectNode) hit );
			ObjectNode source = HIT_SOURCE_ACCESSOR.get( hitObject ).orElse( null );
			extractedData.add( rootExtractor.extract(
					hitMapper, hitObject, source, projectionExtractContext
			) );
		}

		return extractedData;
	}

	private Map<AggregationKey<?>, ?> extractAggregations(ElasticsearchSearchQueryExtractContext extractContext,
			ObjectNode responseBody) {
		ObjectNode jsonAggregations =
				AGGREGATIONS_ACCESSOR.get( responseBody ).orElseGet( JsonNodeFactory.instance::objectNode );

		Map<AggregationKey<?>, Object> extractedMap = new LinkedHashMap<>();

		for ( ElasticsearchSearchAggregation.Extractor<?> extractor : aggregations ) {
			Object extracted = extractor.extract( jsonAggregations, extractContext );
			extractedMap.put( extractor.key(), extracted );
		}

		return extractedMap;
	}

	protected String extractScrollId(ObjectNode responseBody) {
		return SCROLL_ID_ACCESSOR.get( responseBody ).orElse( null );
	}
}
