/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
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
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.backend.lucene.work.impl.SingleDocumentIndexingWork;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.common.spi.MultiEntityOperationExecutionReport;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;

public class LuceneIndexIndexingPlan implements IndexIndexingPlan {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final EntityReferenceFactory entityReferenceFactory;
	private final String tenantId;
	private final DocumentCommitStrategy commitStrategy;
	private final DocumentRefreshStrategy refreshStrategy;

	private final Map<LuceneSerialWorkOrchestrator, List<SingleDocumentIndexingWork>> worksByOrchestrator = new HashMap<>();

	public LuceneIndexIndexingPlan(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			LuceneIndexEntryFactory indexEntryFactory,
			BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.entityReferenceFactory = sessionContext.mappingContext().entityReferenceFactory();
		this.tenantId = sessionContext.tenantIdentifier();
		this.commitStrategy = commitStrategy;
		this.refreshStrategy = refreshStrategy;
	}

	@Override
	public void add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		collect( id, routingKey, factory.add(
				tenantId, indexManagerContext.mappedTypeName(), referenceProvider.entityIdentifier(),
				id, indexEntry
		) );
	}

	@Override
	public void addOrUpdate(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		collect( id, routingKey, factory.update(
				tenantId, indexManagerContext.mappedTypeName(), referenceProvider.entityIdentifier(),
				id, indexEntry
		) );
	}

	@Override
	public void delete(DocumentReferenceProvider referenceProvider) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		collect( id, routingKey, factory.delete(
				tenantId, indexManagerContext.mappedTypeName(), referenceProvider.entityIdentifier(),
				id
		) );
	}

	@Override
	public CompletableFuture<MultiEntityOperationExecutionReport> executeAndReport(OperationSubmitter operationSubmitter) {
		try {
			List<CompletableFuture<MultiEntityOperationExecutionReport>> shardReportFutures = new ArrayList<>();
			for ( Map.Entry<LuceneSerialWorkOrchestrator, List<SingleDocumentIndexingWork>> entry : worksByOrchestrator
					.entrySet() ) {
				LuceneSerialWorkOrchestrator orchestrator = entry.getKey();
				List<SingleDocumentIndexingWork> works = entry.getValue();
				LuceneIndexIndexingPlanExecution execution = new LuceneIndexIndexingPlanExecution(
						orchestrator, entityReferenceFactory,
						commitStrategy, refreshStrategy,
						works
				);
				shardReportFutures.add( execution.execute( operationSubmitter ) );
			}
			return MultiEntityOperationExecutionReport.allOf( shardReportFutures );
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
		LuceneSerialWorkOrchestrator orchestrator = indexManagerContext.indexingOrchestrator( documentId, routingKey );

		List<SingleDocumentIndexingWork> works = worksByOrchestrator.get( orchestrator );
		if ( works == null ) {
			works = new ArrayList<>();
			worksByOrchestrator.put( orchestrator, works );
		}
		works.add( work );
	}
}
