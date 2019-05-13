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


/**
 * @author Guillaume Smet
 */
public class LuceneStubWorkFactory implements LuceneWorkFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneStubWorkFactory(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public LuceneIndexWork<?> add(String indexName, String tenantId, String id, String routingKey, LuceneIndexEntry indexEntry) {
		return new LuceneAddEntryWork( indexName, tenantId, id, indexEntry );
	}

	@Override
	public LuceneIndexWork<?> update(String indexName, String tenantId, String id, String routingKey,
			LuceneIndexEntry indexEntry) {
		return multiTenancyStrategy.createUpdateEntryLuceneWork( indexName, tenantId, id, indexEntry );
	}

	@Override
	public LuceneIndexWork<?> delete(String indexName, String tenantId, String id, String routingKey) {
		return multiTenancyStrategy.createDeleteEntryLuceneWork( indexName, tenantId, id );
	}

	@Override
	public LuceneIndexWork<?> deleteAll(String indexName, String tenantId) {
		return multiTenancyStrategy.createDeleteAllEntriesLuceneWork( indexName, tenantId );
	}

	@Override
	public LuceneIndexWork<?> flush(String indexName) {
		return new LuceneFlushIndexWork( indexName );
	}

	@Override
	public LuceneIndexWork<?> commit(String indexName) {
		return new LuceneCommitIndexWork( indexName );
	}

	@Override
	public LuceneIndexWork<?> optimize(String indexName) {
		return new LuceneOptimizeIndexWork( indexName );
	}

	@Override
	public <H> LuceneQueryWork<LuceneLoadableSearchResult<H>> search(Set<String> indexNames, Query luceneQuery, Sort luceneSort,
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
}
