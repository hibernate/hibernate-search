/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Explanation;


public interface LuceneWorkFactory {

	LuceneSingleDocumentWriteWork<?> add(String tenantId, String id, LuceneIndexEntry indexEntry);

	LuceneSingleDocumentWriteWork<?> update(String tenantId, String id, LuceneIndexEntry indexEntry);

	LuceneSingleDocumentWriteWork<?> delete(String tenantId, String id);

	LuceneWriteWork<?> deleteAll(String tenantId);

	LuceneWriteWork<?> noOp();

	LuceneWriteWork<?> mergeSegments();

	<R> LuceneReadWork<R> search(LuceneSearcher<R> searcher, Integer offset, Integer limit);

	LuceneReadWork<Integer> count(LuceneSearcher<?> searcher);

	LuceneReadWork<Explanation> explain(LuceneSearcher<?> searcher,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentFilter);
}
