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

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlanExecutionReport;

public class LuceneIndexIndexingPlan<R> implements IndexIndexingPlan<R> {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;
	private final EntityReferenceFactory<R> entityReferenceFactory;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final Map<LuceneSerialWorkOrchestrator, List<SingleDocumentIndexingWork>> worksByOrchestrator = new HashMap<>();

	public LuceneIndexIndexingPlan(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			LuceneIndexEntryFactory indexEntryFactory,
			BackendSessionContext sessionContext,
			EntityReferenceFactory<R> entityReferenceFactory,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.getTenantIdentifier();
		this.entityReferenceFactory = entityReferenceFactory;
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		collect( id, routingKey, factory.add(
				tenantId, indexManagerContext.getMappedTypeName(), referenceProvider.getEntityIdentifier(),
				indexEntry
		) );
	}

	@Override
	public void update(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		collect( id, routingKey, factory.update(
				tenantId, indexManagerContext.getMappedTypeName(), referenceProvider.getEntityIdentifier(),
				id, indexEntry
		) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.getIdentifier();
		String routingKey = referenceProvider.getRoutingKey();

		collect( id, routingKey, factory.delete(
				tenantId, indexManagerContext.getMappedTypeName(), referenceProvider.getEntityIdentifier(),
				id
		) );
	}

	@Override
	public void process() {
		// Nothing to do: we only have to send the works to the orchestrator
	}

	@Override
	public CompletableFuture<IndexIndexingPlanExecutionReport<R>> executeAndReport() {
		try {
			List<CompletableFuture<IndexIndexingPlanExecutionReport<R>>> shardReportFutures = new ArrayList<>();
			for ( Map.Entry<LuceneSerialWorkOrchestrator, List<SingleDocumentIndexingWork>> entry : worksByOrchestrator.entrySet() ) {
				LuceneSerialWorkOrchestrator orchestrator = entry.getKey();
				List<SingleDocumentIndexingWork> works = entry.getValue();
				LuceneIndexIndexingPlanExecution<R> execution = new LuceneIndexIndexingPlanExecution<>(
						orchestrator, entityReferenceFactory,
						commitStrategy, refreshStrategy,
						works
				);
				shardReportFutures.add( execution.execute() );
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

	private void collect(String documentId, String routingKey, SingleDocumentIndexingWork work) {
		// Route the work to the appropriate shard
		LuceneSerialWorkOrchestrator orchestrator = indexManagerContext.getIndexingOrchestrator( documentId, routingKey );

		List<SingleDocumentIndexingWork> works = worksByOrchestrator.get( orchestrator );
		if ( works == null ) {
			works = new ArrayList<>();
			worksByOrchestrator.put( orchestrator, works );
		}
		works.add( work );
	}
}
