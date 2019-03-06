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
	 * Execute the query and retrieve the results as a {@link SearchResult}.
	 *
	 * @return The results.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	SearchResult<T> execute();

	/**
	 * Execute the query and retrieve the total hit count,
	 * ignoring pagination settings ({@link #setMaxResults(Long)} and {@link #setFirstResult(Long)}).
	 *
	 * @return The total number of matching entities, ignoring pagination settings.
	 * @throws org.hibernate.search.util.common.SearchException If something goes wrong while executing the query.
	 */
	long executeCount();

	/**
	 * Set the maximum number of results returned by this query.
	 * <p>
	 * The default is no limit.
	 *
	 * @param maxResults The maximum number of results to return. Must be positive or zero; {@code null} resets to the default.
	 */
	void setMaxResults(Long maxResults);

	/**
	 * Set the offset of the first result returned by this query.
	 * <p>
	 * The default offset is {@code 0}.
	 *
	 * @param firstResultIndex The offset of the first result. Must be positive or zero; {@code null} resets to the default.
	 */
	void setFirstResult(Long firstResultIndex);

	/**
	 * @return A textual representation of the query.
	 */
	String getQueryString();

}
