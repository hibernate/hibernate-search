/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.work.impl.LuceneSingleDocumentWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;

public class LuceneIndexIndexingPlan implements IndexIndexingPlan<LuceneRootDocumentBuilder> {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final Map<LuceneWriteWorkOrchestrator, List<LuceneSingleDocumentWriteWork<?>>> worksByOrchestrator = new HashMap<>();

	public LuceneIndexIndexingPlan(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			LuceneIndexEntryFactory indexEntryFactory,
			BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		collect( id, routingKey, factory.add( tenantId, id, indexEntry ) );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor<LuceneRootDocumentBuilder> documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		collect( id, routingKey, factory.update( tenantId, id, indexEntry ) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		collect( id, routingKey, factory.delete( tenantId, id ) );
	}

	@Override
	public void process() {
		// Nothing to do: we only have to send the works to the orchestrator
	}

	@Override
	public CompletableFuture<IndexIndexingPlanExecutionReport> executeAndReport() {
		try {
			List<CompletableFuture<IndexIndexingPlanExecutionReport>> shardReportFutures = new ArrayList<>();
			for ( Map.Entry<LuceneWriteWorkOrchestrator, List<LuceneSingleDocumentWriteWork<?>>> entry : worksByOrchestrator.entrySet() ) {
				LuceneWriteWorkOrchestrator orchestrator = entry.getKey();
				List<LuceneSingleDocumentWriteWork<?>> works = entry.getValue();
				CompletableFuture<IndexIndexingPlanExecutionReport> shardReportFuture = new CompletableFuture<>();
				orchestrator.submit( new LuceneIndexingPlanWriteWorkSet(
						indexManagerContext.getIndexName(), works, shardReportFuture, commitStrategy, refreshStrategy
				) );
				shardReportFutures.add( shardReportFuture );
			}
			return IndexIndexingPlanExecutionReport.allOf( shardReportFutures );
		}
		finally {
			worksByOrchestrator.clear();
		}
	}

	@Override
	public void discard() {
		worksByOrchestrator.clear();
	}

	private void collect(String documentId, String routingKey, LuceneSingleDocumentWriteWork<?> work) {
		// Route the work to the appropriate shard
		LuceneWriteWorkOrchestrator orchestrator = indexManagerContext.getWriteOrchestrator( documentId, routingKey );

		List<LuceneSingleDocumentWriteWork<?>> works = worksByOrchestrator.get( orchestrator );
		if ( works == null ) {
			works = new ArrayList<>();
			worksByOrchestrator.put( orchestrator, works );
		}
		works.add( work );
	}
}
