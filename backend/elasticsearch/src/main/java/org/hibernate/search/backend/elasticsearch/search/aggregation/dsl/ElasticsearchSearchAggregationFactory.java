package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl;

import org.hibernate.search.backend.elasticsearch.gson.GsonJsonHandlers;
import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;
import org.hibernate.search.util.json.JsonHandler;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchAggregationFactory<SR>
		extends
		ExtendedSearchAggregationFactory<SR,
				ElasticsearchSearchAggregationFactory<SR>,
				ElasticsearchSearchPredicateFactory<SR>> {

	/**
	 * Create an aggregation from JSON.
	 * <p>
	 * The created aggregation will return the result as a {@link JsonObject}.
	 *
	 * @param jsonObject A {@link JsonObject} representing an Elasticsearch aggregation.
	 * The JSON object must be a syntactically correct Elasticsearch aggregation.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html">the Elasticsearch documentation</a>.
	 * @return The final step of the aggregation DSL.
	 */
	@Deprecated
	default AggregationFinalStep<JsonObject> fromJson(JsonObject jsonObject) {
		return fromJson( GsonJsonHandlers.object(), jsonObject );
	}

	/**
	 * Create an aggregation from JSON.
	 * <p>
	 * The created aggregation will return the result as a {@link JsonObject}.
	 *
	 * @param jsonString A JSON-formatted string representing an Elasticsearch aggregation.
	 * The JSON object must be a syntactically correct Elasticsearch aggregation.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html">the Elasticsearch documentation</a>.
	 * @return The final step of the aggregation DSL.
	 */
	@Deprecated
	default AggregationFinalStep<JsonObject> fromJson(String jsonString) {
		return fromJson( jsonString, GsonJsonHandlers.object() );
	}

	<T> AggregationFinalStep<T> fromJson(JsonHandler<T> handler, T jsonObject);

	<T> AggregationFinalStep<T> fromJson(String jsonString, JsonHandler<T> resultHandler);

}
