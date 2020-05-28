/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query;

import java.util.List;

/**
 * @param <H> The type of hits.
 */
public interface SearchScrollResult<H> {

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

	// TODO Restore took/timeout for scrolling results
	//	Duration took();
	//	boolean timedOut();

}
