/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.query.spi;

import org.hibernate.search.engine.search.query.SearchQuery;
import org.hibernate.search.util.common.SearchException;

/**
 * @param <T> The type of results.
 */
public interface IndexSearchQuery<T> extends SearchQuery<T> {

	@Override
	IndexSearchResult<T> fetch();

	@Override
	IndexSearchResult<T> fetch(Long limit, Long offset);

	@Override
	String getQueryString();

	/**
	 * Extend the current query with the given extension,
	 * resulting in an extended query offering more options or a more detailed result type.
	 *
	 * @param extension The extension to the predicate DSL.
	 * @param <Q> The type of queries provided by the extension.
	 * @return The extended query.
	 * @throws SearchException If the extension cannot be applied (wrong underlying backend, ...).
	 */
	<Q> Q extension(IndexSearchQueryExtension<Q, T> extension);

}
