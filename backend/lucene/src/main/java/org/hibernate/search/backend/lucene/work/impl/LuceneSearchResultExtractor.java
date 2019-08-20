/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.io.IOException;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TopDocs;

import org.hibernate.search.backend.lucene.search.projection.impl.SearchProjectionExtractContext;

public interface LuceneSearchResultExtractor<R> {

	R extract(IndexSearcher indexSearcher, long totalHits, TopDocs topDocs,
			SearchProjectionExtractContext projectionExecutionContext) throws IOException;

}
