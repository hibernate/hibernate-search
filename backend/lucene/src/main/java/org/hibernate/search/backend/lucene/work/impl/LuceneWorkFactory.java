/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Explanation;


public interface LuceneWorkFactory {

	LuceneWriteWork<?> add(String tenantId, String id, LuceneIndexEntry indexEntry);

	LuceneWriteWork<?> update(String tenantId, String id, LuceneIndexEntry indexEntry);

	LuceneWriteWork<?> delete(String tenantId, String id);

	LuceneWriteWork<?> deleteAll(String tenantId);

	LuceneWriteWork<?> flush();

	LuceneWriteWork<?> optimize();

	<R> LuceneReadWork<R> search(Query luceneQuery, Sort luceneSort,
			Integer offset, Integer limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<R> searchResultExtractor);

	LuceneReadWork<Explanation> explain(Query luceneQuery,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentQuery);
}
