/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.lowlevel.common.impl.MetadataFields;
import org.hibernate.search.backend.lucene.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;

import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;

public class LuceneWorkFactoryImpl implements LuceneWorkFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneWorkFactoryImpl(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	@Override
	public IndexManagementWork<Void> createIndexIfMissing() {
		return new CreateIndexIfMissingWork();
	}

	@Override
	public IndexManagementWork<Void> dropIndexIfExisting() {
		return new DropIndexIfExistingWork();
	}

	@Override
	public IndexManagementWork<Void> validateIndexExists() {
		return new ValidateIndexExistsWork();
	}

	@Override
	public IndexManagementWork<?> flush() {
		return new FlushWork();
	}

	@Override
	public IndexManagementWork<?> refresh() {
		return new RefreshWork();
	}

	@Override
	public IndexManagementWork<?> mergeSegments() {
		return new MergeSegmentsWork();
	}

	@Override
	public SingleDocumentWriteWork add(String tenantId, String entityTypeName, Object entityIdentifier,
			LuceneIndexEntry indexEntry) {
		return new AddEntryWork( tenantId, entityTypeName, entityIdentifier, indexEntry );
	}

	@Override
	public SingleDocumentWriteWork update(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry) {
		Query filter = multiTenancyStrategy.getFilterOrNull( tenantId );
		return new UpdateEntryWork( tenantId, entityTypeName, entityIdentifier,
				documentIdentifier, filter, indexEntry );
	}

	@Override
	public SingleDocumentWriteWork delete(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier) {
		Query filter = multiTenancyStrategy.getFilterOrNull( tenantId );
		return new DeleteEntryWork( tenantId, entityTypeName, entityIdentifier, documentIdentifier, filter );
	}

	@Override
	public IndexManagementWork<?> deleteAll(String tenantId, Set<String> routingKeys) {
		List<Query> filters = new ArrayList<>();
		Query filter = multiTenancyStrategy.getFilterOrNull( tenantId );
		if ( filter != null ) {
			filters.add( filter );
		}
		if ( !routingKeys.isEmpty() ) {
			filters.add( Queries.anyTerm( MetadataFields.routingKeyFieldName(), routingKeys ) );
		}

		return new DeleteEntriesByQueryWork( Queries.boolFilter( new MatchAllDocsQuery(), filters ) );
	}

	@Override
	public <R> ReadWork<R> search(LuceneSearcher<R> searcher, Integer offset, Integer limit) {
		return new SearchWork<>( searcher, offset, limit );
	}

	@Override
	public ReadWork<Integer> count(LuceneSearcher<?> searcher) {
		return new CountWork( searcher );
	}

	@Override
	public ReadWork<Explanation> explain(LuceneSearcher<?> searcher,
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentFilter) {
		return new ExplainWork(
				searcher,
				explainedDocumentIndexName, explainedDocumentId, explainedDocumentFilter
		);
	}
}
