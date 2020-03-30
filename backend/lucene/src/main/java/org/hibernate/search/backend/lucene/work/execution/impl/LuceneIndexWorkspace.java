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
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;

public class LuceneIndexWorkspace implements IndexWorkspace {

	private final LuceneWorkFactory factory;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final DetachedBackendSessionContext sessionContext;

	public LuceneIndexWorkspace(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			DetachedBackendSessionContext sessionContext) {
		this.factory = factory;
		this.indexManagerContext = indexManagerContext;
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> mergeSegments() {
		return doSubmit( indexManagerContext.getAllManagementOrchestrators(), factory.mergeSegments(), false );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys) {
		return doSubmit(
				indexManagerContext.getManagementOrchestrators( routingKeys ),
				factory.deleteAll( sessionContext.getTenantIdentifier(), routingKeys ),
				true
		);
	}

	@Override
	public CompletableFuture<?> flush() {
		return doSubmit( indexManagerContext.getAllManagementOrchestrators(), factory.flush(), false );
	}

	@Override
	public CompletableFuture<?> refresh() {
		return doSubmit( indexManagerContext.getAllManagementOrchestrators(), factory.refresh(), false );
	}

	private <T> CompletableFuture<?> doSubmit(List<LuceneParallelWorkOrchestrator> orchestrators,
			IndexManagementWork<T> work, boolean commit) {
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

			orchestrator.submit( writeFuture, work );
		}
		return CompletableFuture.allOf( writeAndCommitFutures );
	}
}
