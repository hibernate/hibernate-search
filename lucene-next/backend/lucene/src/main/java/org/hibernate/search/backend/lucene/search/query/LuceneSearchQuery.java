/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchQuery;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Sort;

public interface LuceneSearchQuery<H>
		extends ExtendedSearchQuery<H, LuceneSearchResult<H>, LuceneSearchScroll<H>>, LuceneSearchFetchable<H> {

	/**
	 * Explain score computation of this query for the document with the given id.
	 * <p>
	 * This is a shorthand for {@link #explain(String, Object)} when the query only targets one mapped type.
	 *
	 * @param id The id of the entity whose score should be explained.
	 * This is the entity ID, which may be of any type ({@code long}, ...),
	 * not the document ID which is always a string.
	 * @return An {@link org.apache.lucene.search.Explanation} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the query targets multiple mapped types,
	 * or if the explain request fails.
	 */
	Explanation explain(Object id);

	/**
	 * Explain score computation of this query for the document with the given id in the given mapped type.
	 * <p>
	 * This feature is relatively expensive, use it only sparingly and when you need to debug a slow query.
	 *
	 * @param typeName The name of the type of the entity whose score should be explained.
	 * @param id The id of the entity whose score should be explained.
	 * This is the entity ID, which may be of any type ({@code long}, ...),
	 * not the document ID which is always a string.
	 * @return An {@link org.apache.lucene.search.Explanation} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the given index name does not refer to a mapped name targeted by this query,
	 * or if the explain request fails.
	 */
	Explanation explain(String typeName, Object id);

	/**
	 * @return The Lucene {@link org.apache.lucene.search.Sort} this query relies on.
	 */
	Sort luceneSort();

}
