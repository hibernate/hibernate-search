/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.dsl;

import org.hibernate.search.engine.search.predicate.dsl.ExtendedSearchPredicateFactory;
import org.hibernate.search.engine.search.predicate.dsl.PredicateFinalStep;

import com.google.gson.JsonObject;

/**
 * A factory for search predicates with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchPredicateFactory
		extends ExtendedSearchPredicateFactory<ElasticsearchSearchPredicateFactory> {

	/**
	 * Create a predicate from JSON.
	 *
	 * @param jsonString A JSON-formatted string representing an Elasticsearch query.
	 * The JSON object must be a syntactically correct Elasticsearch query.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">the Elasticsearch documentation</a>.
	 * @return The final step of the predicate DSL.
	 */
	PredicateFinalStep fromJson(String jsonString);

	/**
	 * Create a predicate from JSON.
	 *
	 * @param jsonObject A {@link JsonObject} representing an Elasticsearch query.
	 * The JSON object must be a syntactically correct Elasticsearch query.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html">the Elasticsearch documentation</a>.
	 * @return The final step of the predicate DSL.
	 */
	PredicateFinalStep fromJson(JsonObject jsonObject);

}
