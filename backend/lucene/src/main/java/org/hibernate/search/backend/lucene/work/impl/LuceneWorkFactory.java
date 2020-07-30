/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Query;


public interface LuceneWorkFactory {

	IndexManagementWork<Void> createIndexIfMissing();

	IndexManagementWork<Void> dropIndexIfExisting();

	IndexManagementWork<Void> validateIndexExists();

	IndexManagementWork<?> flush();

	IndexManagementWork<?> refresh();

	IndexManagementWork<?> mergeSegments();

	SingleDocumentIndexingWork add(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry);

	SingleDocumentIndexingWork update(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry);

	SingleDocumentIndexingWork delete(String tenantId, String entityTypeName, Object entityIdentifier, String id);

	IndexManagementWork<?> deleteAll(String tenantId, Set<String> routingKeys);

	<R> ReadWork<R> search(LuceneSearcher<R, ?> searcher, Integer offset, Integer limit);

	<ER> ReadWork<ER> scroll(LuceneSearcher<?, ER> searcher, Integer offset, Integer limit);

	ReadWork<Integer> count(LuceneSearcher<?, ?> searcher);

	ReadWork<Explanation> explain(LuceneSearcher<?, ?> searcher,
			String explainedDocumentTypeName, String explainedDocumentId, Query explainedDocumentFilter);
}
