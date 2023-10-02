/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchQuery;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchQuery<H>
		extends ExtendedSearchQuery<H, ElasticsearchSearchResult<H>, ElasticsearchSearchScroll<H>>,
		ElasticsearchSearchFetchable<H> {

	/**
	 * Explain score computation of this query for the document with the given id.
	 * <p>
	 * This is a shorthand for {@link #explain(String, Object)} when the query only targets one index.
	 *
	 * @param id The id of the entity whose score should be explained.
	 * This is the entity ID, which may be of any type ({@code long}, ...),
	 * not the document ID which is always a string.
	 * @return A {@link JsonObject} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the query targets multiple indexes,
	 * or if the explain request fails.
	 */
	JsonObject explain(Object id);

	/**
	 * Explain score computation of this query for the document with the given id in the given mapped type.
	 * <p>
	 * This feature is relatively expensive, use it only sparingly and when you need to debug a slow query.
	 *
	 * @param typeName The name of the type of the entity whose score should be explained.
	 * @param id The id of the entity whose score should be explained.
	 * This is the entity ID, which may be of any type ({@code long}, ...),
	 * not the document ID which is always a string.
	 * @return A {@link JsonObject} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the given mapped type name does not refer to a mapped name targeted by this query,
	 * or if the explain request fails.
	 */
	JsonObject explain(String typeName, Object id);

}
