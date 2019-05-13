/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneLoadableSearchResult;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchResultExtractor;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Explanation;


/**
 * @author Guillaume Smet
 */
public class LuceneStubWorkFactory implements LuceneWorkFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneStubWorkFactory(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public LuceneWriteWork<?> add(String indexName, String tenantId, String id, String routingKey, LuceneIndexEntry indexEntry) {
		return new LuceneAddEntryWork( indexName, tenantId, id, indexEntry );
	}

	@Override
	public LuceneWriteWork<?> update(String indexName, String tenantId, String id, String routingKey,
			LuceneIndexEntry indexEntry) {
		return multiTenancyStrategy.createUpdateEntryLuceneWork( indexName, tenantId, id, indexEntry );
	}

	@Override
	public LuceneWriteWork<?> delete(String indexName, String tenantId, String id, String routingKey) {
		return multiTenancyStrategy.createDeleteEntryLuceneWork( indexName, tenantId, id );
	}

	@Override
	public LuceneWriteWork<?> deleteAll(String indexName, String tenantId) {
		return multiTenancyStrategy.createDeleteAllEntriesLuceneWork( indexName, tenantId );
	}

	@Override
	public LuceneWriteWork<?> flush(String indexName) {
		return new LuceneFlushWork( indexName );
	}

	@Override
	public LuceneWriteWork<?> commit(String indexName) {
		return new LuceneCommitWork( indexName );
	}

	@Override
	public LuceneWriteWork<?> optimize(String indexName) {
		return new LuceneOptimizeWork( indexName );
	}

	@Override
	public <H> LuceneReadWork<LuceneLoadableSearchResult<H>> search(Set<String> indexNames, Query luceneQuery, Sort luceneSort,
			Long offset, Long limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<H> searchResultExtractor) {
		return new LuceneSearchWork<>(
				indexNames, luceneQuery, luceneSort,
				offset, limit,
				luceneCollectorProvider,
				searchResultExtractor
		);
	}

	@Override
	public LuceneReadWork<Explanation> explain(Set<String> indexNames, Query luceneQuery,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentQuery) {
		return new LuceneExplainWork(
				indexNames, luceneQuery,
				explainedDocumentIndexName, explainedDocumentId, explainedDocumentQuery
		);
	}
}
