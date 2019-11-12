/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.dsl;

import org.hibernate.search.engine.search.aggregation.dsl.AggregationFinalStep;
import org.hibernate.search.engine.search.aggregation.dsl.SearchAggregationFactory;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchAggregationFactory extends SearchAggregationFactory {

	/**
	 * Create an aggregation from JSON.
	 * <p>
	 * The created aggregation will return the result as a JSON-formatted string.
	 *
	 * @param jsonObject A {@link JsonObject} representing an Elasticsearch aggregation.
	 * The JSON object must be a syntactically correct Elasticsearch aggregation.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html">the Elasticsearch documentation</a>.
	 * @return The final step of the aggregation DSL.
	 */
	AggregationFinalStep<String> fromJson(JsonObject jsonObject);

	/**
	 * Create an aggregation from JSON.
	 * <p>
	 * The created aggregation will return the result as a JSON-formatted string.
	 *
	 * @param jsonString A JSON-formatted string representing an Elasticsearch aggregation.
	 * The JSON object must be a syntactically correct Elasticsearch aggregation.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html">the Elasticsearch documentation</a>.
	 * @return The final step of the aggregation DSL.
	 */
	AggregationFinalStep<String> fromJson(String jsonString);

}
