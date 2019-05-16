/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneLoadableSearchResult;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchResultExtractor;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Explanation;

/**
 * @author Guillaume Smet
 */
public interface LuceneWorkFactory {

	LuceneWriteWork<?> add(String indexName, String tenantId, String id, String routingKey, LuceneIndexEntry indexEntry);

	LuceneWriteWork<?> update(String indexName, String tenantId, String id, String routingKey,
			LuceneIndexEntry indexEntry);

	LuceneWriteWork<?> delete(String indexName, String tenantId, String id, String routingKey);

	LuceneWriteWork<?> deleteAll(String indexName, String tenantId);

	LuceneWriteWork<?> commit(String indexName);

	LuceneWriteWork<?> flush(String indexName);

	LuceneWriteWork<?> optimize(String indexName);

	<H> LuceneReadWork<LuceneLoadableSearchResult<H>> search(
			Set<String> indexNames, Query luceneQuery, Sort luceneSort,
			Integer offset, Integer limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<H> searchResultExtractor);

	LuceneReadWork<Explanation> explain(Set<String> indexNames, Query luceneQuery,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentQuery);
}
