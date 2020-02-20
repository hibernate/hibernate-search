/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
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
		return doSubmit(
				indexManagerContext.getAllWriteOrchestrators(), factory.mergeSegments(),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys) {
		return doSubmit(
				indexManagerContext.getWriteOrchestrators( routingKeys ),
				factory.deleteAll( sessionContext.getTenantIdentifier(), routingKeys ),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> flush() {
		return doSubmit(
				indexManagerContext.getAllWriteOrchestrators(), factory.noOp(),
				DocumentCommitStrategy.FORCE, DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> refresh() {
		return doSubmit(
				indexManagerContext.getAllWriteOrchestrators(), factory.noOp(),
				DocumentCommitStrategy.NONE, DocumentRefreshStrategy.FORCE
		);
	}

	private CompletableFuture<?> doSubmit(Collection<LuceneWriteWorkOrchestrator> orchestrators,
			LuceneWriteWork<?> work,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		CompletableFuture<?>[] futures = new CompletableFuture[orchestrators.size()];
		int i = 0;
		for ( LuceneWriteWorkOrchestrator orchestrator : orchestrators ) {
			futures[i] = orchestrator.submit(
					work,
					commitStrategy,
					refreshStrategy
			);
			++i;
		}
		return CompletableFuture.allOf( futures );
	}
}
