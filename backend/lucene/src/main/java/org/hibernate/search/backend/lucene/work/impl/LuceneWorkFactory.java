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

/**
 * @author Guillaume Smet
 */
public interface LuceneWorkFactory {

	LuceneIndexWork<?> add(String indexName, String tenantId, String id, String routingKey, LuceneIndexEntry indexEntry);

	LuceneIndexWork<?> update(String indexName, String tenantId, String id, String routingKey,
			LuceneIndexEntry indexEntry);

	LuceneIndexWork<?> delete(String indexName, String tenantId, String id, String routingKey);

	LuceneIndexWork<?> deleteAll(String indexName, String tenantId);

	LuceneIndexWork<?> commit(String indexName);

	LuceneIndexWork<?> flush(String indexName);

	LuceneIndexWork<?> optimize(String indexName);

	<H> LuceneQueryWork<LuceneLoadableSearchResult<H>> search(
			Set<String> indexNames, Query luceneQuery, Sort luceneSort,
			Long offset, Long limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<H> searchResultExtractor);
}
