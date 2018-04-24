/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearcher;


/**
 * @author Guillaume Smet
 */
public class StubLuceneWorkFactory implements LuceneWorkFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;

	public StubLuceneWorkFactory(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public LuceneIndexWork<?> add(String indexName, String tenantId, String id, String routingKey, LuceneIndexEntry indexEntry) {
		return new AddEntryLuceneWork( indexName, tenantId, id, indexEntry );
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
	public LuceneIndexWork<?> flush(String indexName) {
		return new FlushIndexLuceneWork( indexName );
	}

	@Override
	public LuceneIndexWork<?> commit(String indexName) {
		return new CommitIndexLuceneWork( indexName );
	}

	@Override
	public LuceneIndexWork<?> optimize(String indexName) {
		return new OptimizeIndexLuceneWork( indexName );
	}

	@Override
	public <T> ExecuteQueryLuceneWork<T> search(LuceneSearcher<T> luceneSearcher) {
		return new ExecuteQueryLuceneWork<T>( luceneSearcher );
	}
}
