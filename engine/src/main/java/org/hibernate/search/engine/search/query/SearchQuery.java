/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query;

import org.hibernate.search.util.common.SearchException;

/**
 * A search query, allowing to fetch search results.
 *
 * @param <H> The type of query hits.
 */
public interface SearchQuery<H> extends SearchFetchable<H> {

	/**
	 * @return A textual representation of the query.
	 */
	String queryString();

	/**
	 * @return A textual representation of the query.
	 * @deprecated Use {@link #queryString()} instead.
	 */
	@Deprecated
	default String getQueryString() {
		return queryString();
	}

	/**
	 * Extend the current query with the given extension,
	 * resulting in an extended query offering more options or a more detailed result type.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <Q> The type of queries provided by the extension.
	 * @return The extended query.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<Q> Q extension(SearchQueryExtension<Q, H> extension);

}
