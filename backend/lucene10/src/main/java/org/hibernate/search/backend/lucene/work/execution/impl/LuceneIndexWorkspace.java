/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.IndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.spi.UnsupportedOperationBehavior;

public class LuceneIndexWorkspace implements IndexWorkspace {

	private final LuceneWorkFactory factory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final Set<String> tenantIds;

	public LuceneIndexWorkspace(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			Set<String> tenantIds) {
		this.factory = factory;
		this.indexManagerContext = indexManagerContext;
		this.tenantIds = tenantIds;
	}

	@Override
	public CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter,
			// mergeSegments is always supported
			UnsupportedOperationBehavior ignored) {
		return doSubmit( indexManagerContext.allManagementOrchestrators(), factory.mergeSegments(), false, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter,
			UnsupportedOperationBehavior unsupportedOperationBehavior) {
		return doSubmit(
				indexManagerContext.managementOrchestrators( routingKeys ),
				factory.deleteAll( tenantIds, routingKeys ),
				true, operationSubmitter
		);
	}

	@Override
	public CompletableFuture<?> flush(OperationSubmitter operationSubmitter,
			// flush is always supported
			UnsupportedOperationBehavior ignored) {
		return doSubmit( indexManagerContext.allManagementOrchestrators(), factory.flush(), false, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> refresh(OperationSubmitter operationSubmitter,
			// refresh is always supported
			UnsupportedOperationBehavior ignored) {
		return doSubmit( indexManagerContext.allManagementOrchestrators(), factory.refresh(), false, operationSubmitter );
	}

	private <T> CompletableFuture<?> doSubmit(List<LuceneParallelWorkOrchestrator> orchestrators,
			IndexManagementWork<T> work, boolean commit, OperationSubmitter operationSubmitter) {
		CompletableFuture<?>[] writeFutures = new CompletableFuture[orchestrators.size()];
		CompletableFuture<?>[] writeAndCommitFutures = new CompletableFuture[orchestrators.size()];
		for ( int i = 0; i < writeFutures.length; i++ ) {
			LuceneParallelWorkOrchestrator orchestrator = orchestrators.get( i );

			CompletableFuture<T> writeFuture = new CompletableFuture<>();
			writeFutures[i] = writeFuture;
			if ( commit ) {
				// Add the post-execution action to the future *before* submitting the work,
				// so as to be sure that the commit is executed in the background,
				// not in the current thread.
				// It's important because we don't want to block the current thread.
				writeAndCommitFutures[i] = writeFutures[i].thenRun( orchestrator::forceCommitInCurrentThread );
			}
			else {
				writeAndCommitFutures[i] = writeFuture;
			}

			orchestrator.submit( writeFuture, work, operationSubmitter );
		}
		return CompletableFuture.allOf( writeAndCommitFutures );
	}
}
