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
public interface SearchResult<H> {

	/**
	 * @return The total number of matching entities, ignoring pagination settings.
	 */
	long getTotalHitCount();

	/**
	 * @return The hits as a {@link List} containing one element for each matched entity.
	 */
	List<H> getHits();

}
