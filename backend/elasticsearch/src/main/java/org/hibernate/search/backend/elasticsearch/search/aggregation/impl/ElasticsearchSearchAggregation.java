/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import static org.hibernate.search.util.common.impl.CollectionHelper.isSubset;
import static org.hibernate.search.util.common.impl.CollectionHelper.notInTheOtherSet;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.engine.search.aggregation.AggregationKey;
import org.hibernate.search.engine.search.aggregation.SearchAggregation;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchAggregation<A> extends SearchAggregation<A> {
	/**
	 * Contribute to the request, making sure that the requirements for this aggregation are met.
	 *
	 * @param context The request context, to access context information
	 * or store information that will be made available later to the {@link Extractor#extract(JsonObject, AggregationExtractContext)}
	 * method.
	 * @param key
	 * @return The JSON object representing the aggregation.
	 */
	Extractor<A> request(AggregationRequestContext context, AggregationKey<?> key, JsonObject jsonAggregations);

	Set<String> indexNames();

	interface Extractor<T> {
		/**
		 * Extract the result of the aggregation from the response.
		 *
		 * @param aggregationResult The part of the response body relevant to the aggregation to extract.
		 * @param context The extract context, to extract information from the response's JSON body
		 * or retrieve information that was stored earlier in the {@link #request(AggregationRequestContext, AggregationKey, JsonObject)}
		 * method.
		 * @return The aggregation result extracted from the response.
		 */
		T extract(JsonObject aggregationResult, AggregationExtractContext context);

		/**
		 * @return The aggregation key, the one that this extractor was requested for by {@link ElasticsearchSearchAggregation#request(AggregationRequestContext, AggregationKey, JsonObject)}.
		 */
		AggregationKey<?> key();
	}

	static <A> ElasticsearchSearchAggregation<A> from(ElasticsearchSearchIndexScope<?> scope,
			SearchAggregation<A> aggregation) {
		if ( !( aggregation instanceof ElasticsearchSearchAggregation ) ) {
			throw QueryLog.INSTANCE.cannotMixElasticsearchSearchQueryWithOtherAggregations( aggregation );
		}

		ElasticsearchSearchAggregation<A> casted = (ElasticsearchSearchAggregation<A>) aggregation;
		if ( !isSubset( scope.hibernateSearchIndexNames(), casted.indexNames() ) ) {
			throw QueryLog.INSTANCE.aggregationDefinedOnDifferentIndexes(
					aggregation, casted.indexNames(), scope.hibernateSearchIndexNames(),
					notInTheOtherSet( scope.hibernateSearchIndexNames(), casted.indexNames() )
			);
		}
		return casted;
	}
}
