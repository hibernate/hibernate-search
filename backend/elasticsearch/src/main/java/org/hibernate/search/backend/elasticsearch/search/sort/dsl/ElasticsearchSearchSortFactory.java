/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.sort.dsl;

import org.hibernate.search.backend.elasticsearch.search.predicate.dsl.ElasticsearchSearchPredicateFactory;
import org.hibernate.search.engine.search.sort.dsl.ExtendedSearchSortFactory;
import org.hibernate.search.engine.search.sort.dsl.SortThenStep;

import com.google.gson.JsonObject;

/**
 * A factory for search sorts with some Elasticsearch-specific methods.
 */
public interface ElasticsearchSearchSortFactory
		extends ExtendedSearchSortFactory<ElasticsearchSearchSortFactory, ElasticsearchSearchPredicateFactory> {

	/**
	 * Order elements according to a JSON sort definition.
	 *
	 * @param jsonString A JSON-formatted string representing an Elasticsearch sort.
	 * The JSON object must be a syntactically correct Elasticsearch sort.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html#request-body-search-sort">the Elasticsearch documentation</a>.
	 * @return A {@link SortThenStep} allowing the retrieval of the sort
	 * or the chaining of other sorts.
	 */
	SortThenStep fromJson(String jsonString);

	/**
	 * Order elements according to a JSON sort definition.
	 *
	 * @param jsonObject A {@link JsonObject} representing an Elasticsearch sort.
	 * The JSON object must be a syntactically correct Elasticsearch sort.
	 * See <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-body.html#request-body-search-sort">the Elasticsearch documentation</a>.
	 * @return A {@link SortThenStep} allowing the retrieval of the sort
	 * or the chaining of other sorts.
	 */
	SortThenStep fromJson(JsonObject jsonObject);

}
