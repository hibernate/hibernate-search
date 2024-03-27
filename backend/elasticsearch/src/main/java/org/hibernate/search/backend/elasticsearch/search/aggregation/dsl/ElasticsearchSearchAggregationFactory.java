/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.ExtendedSearchAggregationFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchAggregationFactory
		extends ExtendedSearchAggregationFactory<ElasticsearchSearchAggregationFactory, ElasticsearchSearchPredicateFactory> {

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
	AggregationFinalStep<JsonObject> fromJson(JsonObject jsonObject);

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
	AggregationFinalStep<JsonObject> fromJson(String jsonString);

}
