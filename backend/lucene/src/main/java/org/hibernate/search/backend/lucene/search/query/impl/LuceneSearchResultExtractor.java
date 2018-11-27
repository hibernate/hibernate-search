/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.search.query.impl;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;
import org.hibernate.search.engine.search.SearchResult;

public interface LuceneSearchResultExtractor<T> {

	SearchResult<T> extract(IndexSearcher indexSearcher, long totalHits, TopDocs topDocs) throws IOException;

}
