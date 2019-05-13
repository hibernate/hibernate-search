/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.ExtendedSearchQuery;

import org.apache.lucene.search.Explanation;

public interface LuceneSearchQuery<H> extends ExtendedSearchQuery<H, LuceneSearchResult<H>> {

	/**
	 * Explain score computation of this query for the document with the given id.
	 * <p>
	 * This is a shorthand for {@link #explain(String, String)} when the query only targets one index.
	 *
	 * @param id The id of the document to test.
	 * @return An {@link org.apache.lucene.search.Explanation} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the query targets multiple indexes,
	 * or if the explain request fails.
	 */
	Explanation explain(String id);

	/**
	 * Explain score computation of this query for the document with the given id in the given index.
	 * <p>
	 * This feature is relatively expensive, use it only sparingly and when you need to debug a slow query.
	 *
	 * @param indexName The name of the index containing the document to test.
	 * @param id The id of the document to test.
	 * @return An {@link org.apache.lucene.search.Explanation} describing the score computation for the hit.
	 * @throws org.hibernate.search.util.common.SearchException If the given index name does not refer to an index targeted by this query,
	 * or if the explain request fails.
	 */
	Explanation explain(String indexName, String id);
}
