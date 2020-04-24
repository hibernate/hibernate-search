/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchQuery;

import com.google.gson.JsonObject;

public interface ElasticsearchSearchQuery<H>
		extends ExtendedSearchQuery<H, ElasticsearchSearchResult<H>>, ElasticsearchSearchFetchable<H> {

	/**
	 * Explain score computation of this query for the document with the given id.
	 * <p>
	 * This is a shorthand for {@link #explain(String, String)} when the query only targets one index.
	 *
	 * @param id The id of the document to test.
	 * @return A {@link JsonObject} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the query targets multiple indexes,
	 * or if the explain request fails.
	 */
	JsonObject explain(String id);

	/**
	 * Explain score computation of this query for the document with the given id in the given mapped type.
	 * <p>
	 * This feature is relatively expensive, use it only sparingly and when you need to debug a slow query.
	 *
	 * @param typeName The name of the mapped type containing the document to test.
	 * @param id The id of the document to test.
	 * @return A {@link JsonObject} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the given mapped type name does not refer to a mapped name targeted by this query,
	 * or if the explain request fails.
	 */
	// TODO HSEARCH-3899 avoiding merging index concepts (e.g.: documentId) and entity concepts (e.g.: mapped type)
	JsonObject explain(String typeName, String id);

}
