/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntry;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.IndexingWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;

public class LuceneIndexIndexer implements IndexIndexer {

	private final LuceneWorkFactory factory;
	private final LuceneIndexEntryFactory indexEntryFactory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;

	public LuceneIndexIndexer(LuceneWorkFactory factory,
			LuceneIndexEntryFactory indexEntryFactory,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext) {
		this.factory = factory;
		this.indexEntryFactory = indexEntryFactory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.tenantIdentifier();
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		return submit(
				id, routingKey,
				factory.add(
						tenantId, indexManagerContext.mappedTypeName(),
						referenceProvider.entityIdentifier(), id,
						indexEntry
				),
				commitStrategy, refreshStrategy, operationSubmitter
		);
	}

	@Override
	public CompletableFuture<?> addOrUpdate(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor,
			DocumentCommitStrategy commitStrategy,
			DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();

		LuceneIndexEntry indexEntry = indexEntryFactory.create( tenantId, id, routingKey, documentContributor );

		return submit(
				id, routingKey,
				factory.update(
						tenantId, indexManagerContext.mappedTypeName(),
						referenceProvider.entityIdentifier(), id,
						indexEntry
				),
				commitStrategy, refreshStrategy, operationSubmitter
		);
	}

	@Override
	public CompletableFuture<?> delete(DocumentReferenceProvider referenceProvider,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		String id = referenceProvider.identifier();
		String routingKey = referenceProvider.routingKey();
		return submit(
				id, routingKey,
				factory.delete(
						tenantId, indexManagerContext.mappedTypeName(),
						referenceProvider.entityIdentifier(), id
				),
				commitStrategy, refreshStrategy, operationSubmitter
		);
	}

	private <T> CompletableFuture<T> submit(String documentId, String routingKey, IndexingWork<T> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy,
			OperationSubmitter operationSubmitter) {
		// Route the work to the appropriate shard
		LuceneSerialWorkOrchestrator orchestrator = indexManagerContext.indexingOrchestrator( documentId, routingKey );

		CompletableFuture<T> futureForOrchestrator = new CompletableFuture<>();
		CompletableFuture<T> futureForCaller;

		boolean needsCommit = DocumentCommitStrategy.FORCE.equals( commitStrategy );
		boolean needsRefresh = DocumentRefreshStrategy.FORCE.equals( refreshStrategy );
		if ( needsCommit || needsRefresh ) {
			// Add the handler to the future *before* submitting the works,
			// so as to be sure that the commit/refresh is executed in the background,
			// not in the current thread.
			// It's important because we don't want to block the current thread.
			futureForCaller = futureForOrchestrator.thenApply( result -> {
				if ( needsCommit ) {
					orchestrator.forceCommitInCurrentThread();
				}
				if ( needsRefresh ) {
					orchestrator.forceRefreshInCurrentThread();
				}
				return result;
			} );
		}
		else {
			futureForCaller = futureForOrchestrator;
		}

		orchestrator.submit( futureForOrchestrator, work, operationSubmitter );

		return futureForCaller;
	}
}
