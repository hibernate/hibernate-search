/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.Collection;
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
	public CompletableFuture<?> forceMerge() {
		return doSubmit( factory.forceMerge() );
	}

	@Override
	public CompletableFuture<?> purge() {
		return doSubmit( factory.deleteAll( sessionContext.getTenantIdentifier() ) );
	}

	@Override
	public CompletableFuture<?> flush() {
		return doSubmit( factory.flush() );
	}

	private CompletableFuture<?> doSubmit(LuceneWriteWork<?> work) {
		Collection<LuceneWriteWorkOrchestrator> orchestrators = indexManagerContext.getAllWriteOrchestrators();
		CompletableFuture<?>[] futures = new CompletableFuture[orchestrators.size()];
		int i = 0;
		for ( LuceneWriteWorkOrchestrator orchestrator : orchestrators ) {
			futures[i] = orchestrator.submit(
					work,
					DocumentCommitStrategy.FORCE,
					DocumentRefreshStrategy.NONE
			);
			++i;
		}
		return CompletableFuture.allOf( futures );

	}
}
