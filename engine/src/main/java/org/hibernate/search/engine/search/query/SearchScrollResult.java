/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.query;

import java.time.Duration;
import java.util.List;

/**
 * @param <H> The type of hits.
 */
public interface SearchScrollResult<H> {

	/**
	 * @return The total for a search result, pertaining to all matched documents,
	 * independently from the current chunk of {@link #hits()}. Includes in particular the total hit count.
	 */
	SearchResultTotal total();

	/**
	 * Returns true if this scrolling result contains some index hits.
	 * <p>
	 * Notice that it is possible for this method to return {@code true} while {@link #hits()} returns an empty list,
	 * e.g.: if matching entities could not be found in the database.
	 * <p>
	 * This methods is mainly useful as a stop condition in loops.
	 *
	 * @return if there are still some index result hits.
	 */
	boolean hasHits();

	/**
	 * @return The hits of this scrolling result as a {@link List} containing one element for each matched entity.
	 */
	List<H> hits();

	/**
	 * @return the time taken to process the request, as a {@link Duration}
	 */
	Duration took();

	/**
	 * @return whether or not a timeout occurred processing the request.
	 */
	boolean timedOut();

}
