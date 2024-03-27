/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

import java.time.Duration;
import java.util.List;

import org.hibernate.search.engine.search.aggregation.AggregationKey;

/**
 * @param <H> The type of hits.
 */
public interface SearchResult<H> {

	/**
	 * @return The total for a search result, pertaining to all matched documents,
	 * independently from the offset/limit used when fetching hits. Includes in particular the total hit count.
	 */
	SearchResultTotal total();

	/**
	 * @return The hits as a {@link List} containing one element for each matched entity.
	 */
	List<H> hits();

	/**
	 * @param key The key previously used to register the aggregation during query building.
	 * @param <A> The type of result for this aggregation.
	 * @return The result for the given aggregation.
	 * @throws org.hibernate.search.util.common.SearchException If the given key was never registered
	 * while building this query, and is thus not mapped to anything.
	 */
	<A> A aggregation(AggregationKey<A> key);

	/**
	 * @return the time taken to process the request, as a {@link Duration}
	 */
	Duration took();

	/**
	 * @return whether or not a timeout occurred processing the request.
	 */
	boolean timedOut();

}
