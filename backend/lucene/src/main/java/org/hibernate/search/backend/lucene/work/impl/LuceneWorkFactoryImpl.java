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
	public LuceneSchemaManagementWork<Void> createIndexIfMissing() {
		return new LuceneCreateIndexIfMissingWork();
	}

	@Override
	public LuceneSchemaManagementWork<Void> dropIndexIfExisting() {
		return new LuceneDropIndexIfExistingWork();
	}

	@Override
	public LuceneSchemaManagementWork<Void> validateIndexExists() {
		return new LuceneValidateIndexExistsWork();
	}

	@Override
	public LuceneSingleDocumentWriteWork<?> add(String tenantId, String entityTypeName, Object entityIdentifier,
			LuceneIndexEntry indexEntry) {
		return new LuceneAddEntryWork( tenantId, entityTypeName, entityIdentifier, indexEntry );
	}

	@Override
	public LuceneSingleDocumentWriteWork<?> update(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier, LuceneIndexEntry indexEntry) {
		Query filter = multiTenancyStrategy.getFilterOrNull( tenantId );
		return new LuceneUpdateEntryWork( tenantId, entityTypeName, entityIdentifier,
				documentIdentifier, filter, indexEntry );
	}

	@Override
	public LuceneSingleDocumentWriteWork<?> delete(String tenantId, String entityTypeName, Object entityIdentifier,
			String documentIdentifier) {
		Query filter = multiTenancyStrategy.getFilterOrNull( tenantId );
		return new LuceneDeleteEntryWork( tenantId, entityTypeName, entityIdentifier, documentIdentifier, filter );
	}

	@Override
	public LuceneWriteWork<?> deleteAll(String tenantId, Set<String> routingKeys) {
		List<Query> filters = new ArrayList<>();
		Query filter = multiTenancyStrategy.getFilterOrNull( tenantId );
		if ( filter != null ) {
			filters.add( filter );
		}
		if ( !routingKeys.isEmpty() ) {
			filters.add( Queries.anyTerm( MetadataFields.routingKeyFieldName(), routingKeys ) );
		}

		return new LuceneDeleteEntriesByQueryWork( Queries.boolFilter( new MatchAllDocsQuery(), filters ) );
	}

	@Override
	public LuceneWriteWork<?> noOp() {
		return new LuceneNoOpWriteWork();
	}

	@Override
	public LuceneWriteWork<?> mergeSegments() {
		return new LuceneMergeSegmentsWork();
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
			String explainedDocumentIndexName, String explainedDocumentId, Query explainedDocumentFilter) {
		return new LuceneExplainWork(
				searcher,
				explainedDocumentIndexName, explainedDocumentId, explainedDocumentFilter
		);
	}
}
