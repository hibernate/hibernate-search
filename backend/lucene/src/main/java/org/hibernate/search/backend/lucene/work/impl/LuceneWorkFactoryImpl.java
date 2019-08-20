/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneCollectorProvider;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.Explanation;



public class LuceneWorkFactoryImpl implements LuceneWorkFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneWorkFactoryImpl(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public LuceneWriteWork<?> add(String tenantId, String id,
			LuceneIndexEntry indexEntry) {
		return new LuceneAddEntryWork( tenantId, id, indexEntry );
	}

	@Override
	public LuceneWriteWork<?> update(String tenantId, String id,
			LuceneIndexEntry indexEntry) {
		return multiTenancyStrategy.createUpdateEntryLuceneWork( tenantId, id, indexEntry );
	}

	@Override
	public LuceneWriteWork<?> delete(String tenantId, String id) {
		return multiTenancyStrategy.createDeleteEntryLuceneWork( tenantId, id );
	}

	@Override
	public LuceneWriteWork<?> deleteAll(String tenantId) {
		return multiTenancyStrategy.createDeleteAllEntriesLuceneWork( tenantId );
	}

	@Override
	public LuceneWriteWork<?> flush() {
		return new LuceneFlushWork();
	}

	@Override
	public LuceneWriteWork<?> optimize() {
		return new LuceneOptimizeWork();
	}

	@Override
	public <R> LuceneReadWork<R> search(Query luceneQuery, Sort luceneSort,
			Integer offset, Integer limit,
			LuceneCollectorProvider luceneCollectorProvider,
			LuceneSearchResultExtractor<R> searchResultExtractor) {
		return new LuceneSearchWork<>(
				luceneQuery, luceneSort,
				offset, limit,
				luceneCollectorProvider,
				searchResultExtractor
		);
	}

	@Override
	public LuceneReadWork<Explanation> explain(Query luceneQuery,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentQuery) {
		return new LuceneExplainWork(
				luceneQuery,
				explainedDocumentIndexName, explainedDocumentId, explainedDocumentQuery
		);
	}
}
