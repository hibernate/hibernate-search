/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.impl;

import java.lang.invoke.MethodHandles;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.work.impl.TermBasedDeleteEntryLuceneWork;
import org.hibernate.search.backend.lucene.work.impl.TermBasedUpdateEntryLuceneWork;
import org.hibernate.search.engine.backend.spi.Backend;
import org.hibernate.search.util.impl.common.LoggerFactory;

class NoMultiTenancyStrategyImpl implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean isMultiTenancySupported() {
		return false;
	}

	@Override
	public void contributeToIndexedDocument(Document document, String tenantId) {
	}

	@Override
	public Query decorateLuceneQuery(Query originalLuceneQuery, String tenantId) {
		return originalLuceneQuery;
	}

	@Override
	public TermBasedUpdateEntryLuceneWork createUpdateEntryLuceneWork(String indexName, String tenantId, String id, LuceneIndexEntry indexEntry) {
		return new TermBasedUpdateEntryLuceneWork( indexName, tenantId, id, indexEntry );
	}

	@Override
	public TermBasedDeleteEntryLuceneWork createDeleteEntryLuceneWork(String indexName, String tenantId, String id) {
		return new TermBasedDeleteEntryLuceneWork( indexName, tenantId, id );
	}

	@Override
	public void checkTenantId(Backend<?> backend, String tenantId) {
		if ( tenantId != null ) {
			throw log.tenantIdProvidedButMultiTenancyDisabled( backend, tenantId );
		}
	}
}
