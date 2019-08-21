/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;

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
	public <R> LuceneReadWork<R> search(LuceneSearcher<R> searcher, Integer offset, Integer limit) {
		return new LuceneSearchWork<>( searcher, offset, limit );
	}

	@Override
	public LuceneReadWork<Integer> count(LuceneSearcher<?> searcher) {
		return new LuceneCountWork( searcher );
	}

	@Override
	public LuceneReadWork<Explanation> explain(LuceneSearcher<?> searcher,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentQuery) {
		return new LuceneExplainWork(
				searcher,
				explainedDocumentIndexName, explainedDocumentId, explainedDocumentQuery
		);
	}
}
