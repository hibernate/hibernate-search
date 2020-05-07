/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query;

import org.hibernate.search.engine.search.query.SearchResult;

import org.apache.lucene.search.TopDocs;

public interface LuceneSearchResult<H> extends SearchResult<H> {

	/**
	 * @return the Lucene topDocs produced by the query.
	 * <p>
	 * For the common use cases there is no meaning for using it.
	 * It might be used by an advanced user who needs to merge different query result,
	 * using the merge low level Lucene API, such as {@link TopDocs#merge(int, TopDocs[])}.
	 */
	TopDocs getTopDocs();

}
