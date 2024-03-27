/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
