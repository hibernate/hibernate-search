/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.javabean.search.query;

/**
 * @param <T> The type of results.
 */
public interface SearchQuery<T> {

	/**
	 * Execute the query and return the {@link SearchResult}.
	 *
	 * @return The {@link SearchResult}.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	SearchResult<T> fetch();

	/**
	 * Execute the query and return the total hit count,
	 * ignoring pagination settings ({@link #setMaxResults(Long)} and {@link #setFirstResult(Long)}).
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	long fetchHitCount();

	/**
	 * Set the maximum number of hits returned by this query.
	 * <p>
	 * The default is no limit.
	 *
	 * @param maxResults The maximum number of hits to return. Must be positive or zero; {@code null} resets to the default.
	 * @return {@code this} for method chaining.
	 */
	SearchQuery<T> setMaxResults(Long maxResults);

	/**
	 * Set the offset of the first hit returned by this query.
	 * <p>
	 * The default offset is {@code 0}.
	 *
	 * @param firstResultIndex The offset of the first hit. Must be positive or zero; {@code null} resets to the default.
	 * @return {@code this} for method chaining.
	 */
	SearchQuery<T> setFirstResult(Long firstResultIndex);

	/**
	 * @return A textual representation of the query.
	 */
	String getQueryString();

}
