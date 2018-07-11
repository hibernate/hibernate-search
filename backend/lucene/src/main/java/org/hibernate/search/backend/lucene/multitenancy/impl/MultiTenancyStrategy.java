/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.multitenancy.impl;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Query;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.work.impl.AbstractDeleteEntryLuceneWork;
import org.hibernate.search.backend.lucene.work.impl.AbstractUpdateEntryLuceneWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneIndexWork;
import org.hibernate.search.util.EventContext;

/**
 * Defines how the additional information required by multiTenancy are handled.
 */
public interface MultiTenancyStrategy {

	/**
	 * Indicates if the strategy supports multiTenancy.
	 *
	 * @return {@code true} if multiTenancy is supported, {@code false} otherwise.
	 */
	boolean isMultiTenancySupported();

	/**
	 * Contributes additional information to the indexed document.
	 *
	 * @param document The indexed document.
	 * @param tenantId The tenant id.
	 */
	void contributeToIndexedDocument(Document document, String tenantId);

	/**
	 * Decorate the query with the tenant constraint.
	 *
	 * @param originalLuceneQuery The original Lucene query.
	 * @param tenantId The tenant id.
	 * @return The decorated query.
	 */
	Query decorateLuceneQuery(Query originalLuceneQuery, String tenantId);

	/**
	 * Check that the tenant id value is consistent with the strategy.
	 *
	 * @param tenantId The tenant id.
	 * @param backendContext The backend.
	 */
	void checkTenantId(String tenantId, EventContext backendContext);

	/**
	 * Creates the according update {@link LuceneIndexWork}.
	 *
	 * @param indexName The index name.
	 * @param tenantId The tenant id.
	 * @param id The document id.
	 * @param indexEntry The index entry.
	 * @return The update {@link LuceneIndexWork}.
	 */
	AbstractUpdateEntryLuceneWork createUpdateEntryLuceneWork(String indexName, String tenantId, String id, LuceneIndexEntry indexEntry);

	/**
	 * Creates the according delete {@link LuceneIndexWork}.
	 *
	 * @param indexName The index name.
	 * @param tenantId The tenant id.
	 * @param id The document id.
	 * @return The delete {@link LuceneIndexWork}.
	 */
	AbstractDeleteEntryLuceneWork createDeleteEntryLuceneWork(String indexName, String tenantId, String id);
}
