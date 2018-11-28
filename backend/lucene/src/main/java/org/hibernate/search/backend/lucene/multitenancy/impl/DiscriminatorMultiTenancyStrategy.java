/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.multitenancy.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.impl.LuceneQueries;
import org.hibernate.search.backend.lucene.util.impl.LuceneFields;
import org.hibernate.search.backend.lucene.work.impl.LuceneQueryBasedDeleteEntryWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneQueryBasedUpdateEntryWork;
import org.hibernate.search.util.EventContext;
import org.hibernate.search.util.impl.common.LoggerFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.search.Query;

public class DiscriminatorMultiTenancyStrategy implements MultiTenancyStrategy {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	@Override
	public boolean isMultiTenancySupported() {
		return true;
	}

	@Override
	public void contributeToIndexedDocument(Document document, String tenantId) {
		document.add( new StringField( LuceneFields.tenantIdFieldName(), tenantId, Store.YES ) );
	}

	@Override
	public Query decorateLuceneQuery(Query originalLuceneQuery, String tenantId) {
		return LuceneQueries.wrapWithDiscriminatorTenantIdQuery( originalLuceneQuery, tenantId );
	}

	@Override
	public LuceneQueryBasedUpdateEntryWork createUpdateEntryLuceneWork(String indexName, String tenantId, String id, LuceneIndexEntry indexEntry) {
		return new LuceneQueryBasedUpdateEntryWork( indexName, tenantId, id, indexEntry );
	}

	@Override
	public LuceneQueryBasedDeleteEntryWork createDeleteEntryLuceneWork(String indexName, String tenantId, String id) {
		return new LuceneQueryBasedDeleteEntryWork( indexName, tenantId, id );
	}

	@Override
	public void checkTenantId(String tenantId, EventContext backendContext) {
		if ( tenantId == null ) {
			throw log.multiTenancyEnabledButNoTenantIdProvided( backendContext );
		}
	}
}
