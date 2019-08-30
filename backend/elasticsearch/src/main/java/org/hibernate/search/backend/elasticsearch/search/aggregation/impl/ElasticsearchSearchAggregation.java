/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.aggregation.impl;

import java.util.Set;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchAggregation<A> extends SearchAggregation<A> {

	/**
	 * Contribute to the request, making sure that the requirements for this aggregation are met.
	 *
	 * @param context The request context, to access context information
	 * or store information that will be made available later to the {@link #extract(JsonObject, AggregationExtractContext)}
	 * method.
	 * @return The JSON object representing the aggregation.
	 */
	JsonObject request(AggregationRequestContext context);

	/**
	 * Extract the result of the aggregation from the response.
	 *
	 * @param aggregationResult The part of the response body relevant to the aggregation to extract.
	 * @param context The extract context, to extract information from the response's JSON body
	 * or retrieve information that was stored earlier in the {@link #request(AggregationRequestContext)}
	 * method.
	 * @return The aggregation result extracted from the response.
	 */
	A extract(JsonObject aggregationResult, AggregationExtractContext context);

	Set<String> getIndexNames();

}
