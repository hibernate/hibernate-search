/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;

public class LuceneIndexWorkExecutor implements IndexWorkExecutor {

	private final LuceneWorkFactory factory;
	private final LuceneWriteWorkOrchestrator orchestrator;
	private final DetachedSessionContextImplementor sessionContext;

	public LuceneIndexWorkExecutor(LuceneWorkFactory factory,
			WorkExecutionIndexManagerContext indexManagerContext,
			DetachedSessionContextImplementor sessionContext) {
		this.factory = factory;
		this.orchestrator = indexManagerContext.getWriteOrchestrator();
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> optimize() {
		return orchestrator.submit(
				factory.optimize(),
				DocumentCommitStrategy.FORCE,
				DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> purge() {
		return orchestrator.submit(
				factory.deleteAll( sessionContext.getTenantIdentifier() ),
				DocumentCommitStrategy.FORCE,
				DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> flush() {
		return orchestrator.submit(
				factory.flush(),
				DocumentCommitStrategy.FORCE,
				DocumentRefreshStrategy.NONE
		);
	}
}
