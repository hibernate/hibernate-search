/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @deprecated Use {@link #total()} and then {@link SearchResultTotal#hitCount()} instead.
	 */
	@Deprecated
	default long totalHitCount() {
		return total().hitCount();
	}

	/**
	 * @return The total for a search result, pertaining to all matched documents,
	 * independently from the offset/limit used when fetching hits. Includes in particular the total hit count.
	 */
	SearchResultTotal total();

	/**
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @deprecated Use {@link #total()} and then {@link SearchResultTotal#hitCount()} instead.
	 */
	@Deprecated
	default long getTotalHitCount() {
		return total().hitCount();
	}

	/**
	 * @return The hits as a {@link List} containing one element for each matched entity.
	 */
	List<H> hits();

	/**
	 * @return The hits as a {@link List} containing one element for each matched entity.
	 * @deprecated Use {@link #hits()} instead.
	 */
	@Deprecated
	default List<H> getHits() {
		return hits();
	}

	/**
	 * @param key The key previously used to register the aggregation during query building.
	 * @param <A> The type of result for this aggregation.
	 * @return The result for the given aggregation.
	 * @throws org.hibernate.search.util.common.SearchException If the given key was never registered
	 * while building this query, and is thus not mapped to anything.
	 */
	<A> A aggregation(AggregationKey<A> key);

	/**
	 * @param key The key previously used to register the aggregation during query building.
	 * @param <A> The type of result for this aggregation.
	 * @return The result for the given aggregation.
	 * @throws org.hibernate.search.util.common.SearchException If the given key was never registered
	 * while building this query, and is thus not mapped to anything.
	 * @deprecated Use {@link #aggregation(AggregationKey)} instead.
	 */
	@Deprecated
	default <A> A getAggregation(AggregationKey<A> key) {
		return aggregation( key );
	}

	/**
	 * @return the time taken to process the request, as a {@link Duration}
	 */
	Duration took();

	/**
	 * @return the time taken to process the request, as a {@link Duration}
	 * @deprecated Use {@link #took()} instead.
	 */
	@Deprecated
	default Duration getTook() {
		return took();
	}

	/**
	 * @return whether or not a timeout occurred processing the request.
	 */
	boolean timedOut();

	/**
	 * @return whether or not a timeout occurred processing the request.
	 * @deprecated Use {@link #timedOut()} instead.
	 */
	@Deprecated
	default boolean isTimedOut() {
		return timedOut();
	}

}
