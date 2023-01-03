/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.IndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

public class LuceneIndexWorkspace implements IndexWorkspace {

	private final LuceneWorkFactory factory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;

	public LuceneIndexWorkspace(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			String tenantId) {
		this.factory = factory;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = tenantId;
	}

	@Override
	public CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter) {
		return doSubmit( indexManagerContext.allManagementOrchestrators(), factory.mergeSegments(), false, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter) {
		return doSubmit(
				indexManagerContext.managementOrchestrators( routingKeys ),
				factory.deleteAll( tenantId, routingKeys ),
				true, operationSubmitter
		);
	}

	@Override
	public CompletableFuture<?> flush(OperationSubmitter operationSubmitter) {
		return doSubmit( indexManagerContext.allManagementOrchestrators(), factory.flush(), false, operationSubmitter );
	}

	@Override
	public CompletableFuture<?> refresh(OperationSubmitter operationSubmitter) {
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
