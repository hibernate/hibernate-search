/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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

public final class LuceneWorkFactory {

	private final MultiTenancyStrategy multiTenancyStrategy;

	public LuceneWorkFactory(MultiTenancyStrategy multiTenancyStrategy) {
		this.multiTenancyStrategy = multiTenancyStrategy;
	}

	public IndexManagementWork<Void> createIndexIfMissing() {
		return new CreateIndexIfMissingWork();
	}

	public IndexManagementWork<Void> dropIndexIfExisting() {
		return new DropIndexIfExistingWork();
	}

	public IndexManagementWork<Void> validateIndexExists() {
		return new ValidateIndexExistsWork();
	}

	public IndexManagementWork<?> flush() {
		return new FlushWork();
	}

	public IndexManagementWork<?> refresh() {
		return new RefreshWork();
	}

	public IndexManagementWork<?> mergeSegments() {
		return new MergeSegmentsWork();
	}

	public IndexManagementWork<Long> computeSizeInBytes() {
		return new ComputeSizeInBytesWork();
	}

	public SingleDocumentIndexingWork add(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry) {
		return new AddEntryWork( tenantId, entityTypeName, entityIdentifier,
				documentIdentifier, indexEntry );
	}

	public SingleDocumentIndexingWork update(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry) {
		Query filter = multiTenancyStrategy.filterOrNull( tenantId );
		return new UpdateEntryWork( tenantId, entityTypeName, entityIdentifier,
				documentIdentifier, filter, indexEntry );
	}

	public SingleDocumentIndexingWork delete(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier) {
		Query filter = multiTenancyStrategy.filterOrNull( tenantId );
		return new DeleteEntryWork( tenantId, entityTypeName, entityIdentifier, documentIdentifier, filter );
	}

	public IndexManagementWork<?> deleteAll(Set<String> tenantIds, Set<String> routingKeys) {
		List<Query> filters = new ArrayList<>();
		Query filter = multiTenancyStrategy.filterOrNull( tenantIds );
		if ( filter != null ) {
			filters.add( filter );
		}
		if ( !routingKeys.isEmpty() ) {
			filters.add( Queries.anyTerm( MetadataFields.routingKeyFieldName(), routingKeys ) );
		}

		return new DeleteEntriesByQueryWork( Queries.boolFilter( new MatchAllDocsQuery(), filters ) );
	}

	public <R> ReadWork<R> search(LuceneSearcher<R, ?> searcher, Integer offset, Integer limit, int totalHitCountThreshold) {
		return new SearchWork<>( searcher, offset, limit, totalHitCountThreshold );
	}

	public <ER> ReadWork<ER> scroll(LuceneSearcher<?, ER> searcher, int offset, int limit, int totalHitCountThreshold) {
		return new ScrollWork<>( searcher, offset, limit, totalHitCountThreshold );
	}

	public ReadWork<Integer> count(LuceneSearcher<?, ?> searcher) {
		return new CountWork( searcher );
	}

	public ReadWork<Explanation> explain(LuceneSearcher<?, ?> searcher,
			String explainedDocumentTypeName, String explainedDocumentId, Query explainedDocumentFilter) {
		return new ExplainWork(
				searcher,
				explainedDocumentTypeName, explainedDocumentId, explainedDocumentFilter
		);
	}
}
