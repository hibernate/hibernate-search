/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.aggregation.impl;

import java.io.IOException;
import java.util.Set;

import org.hibernate.search.engine.search.aggregation.SearchAggregation;

public interface LuceneSearchAggregation<A> extends SearchAggregation<A> {

	/**
	 * Request the collection of per-document data that will be used in
	 * {@link #extract(AggregationExtractContext)},
	 * making sure that the requirements for this projection are met.
	 *
	 * @param context A context that will share its state with the context passed to
	 * {@link #extract(AggregationExtractContext)}.
	 */
	void request(AggregationRequestContext context);

	/**
	 * Extract the result of the aggregation from the response.
	 *
	 * @param context The extract context, to extract information from the response's JSON body
	 * or retrieve information that was stored earlier in {@link #request(AggregationRequestContext)}.
	 * @return The aggregation result extracted from the response.
	 */
	A extract(AggregationExtractContext context) throws IOException;

	Set<String> indexNames();

}
