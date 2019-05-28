/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.util.common.reporting.EventContext;

public class LuceneIndexWorkExecutor implements IndexWorkExecutor {

	private final LuceneWorkFactory factory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final LuceneWriteWorkOrchestrator orchestrator;
	private final String indexName;
	private final EventContext eventContext;

	public LuceneIndexWorkExecutor(LuceneWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy, LuceneWriteWorkOrchestrator orchestrator, String indexName,
			EventContext eventContext) {
		this.factory = factory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.eventContext = eventContext;
	}

	@Override
	public CompletableFuture<?> optimize() {
		return orchestrator.submit(
				factory.optimize( indexName ),
				DocumentCommitStrategy.FORCE,
				DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> purge(String tenantId) {
		multiTenancyStrategy.checkTenantId( tenantId, eventContext );
		return orchestrator.submit(
				factory.deleteAll( indexName, tenantId ),
				DocumentCommitStrategy.FORCE,
				DocumentRefreshStrategy.NONE
		);
	}

	@Override
	public CompletableFuture<?> flush() {
		return orchestrator.submit(
				factory.flush( indexName ),
				DocumentCommitStrategy.FORCE,
				DocumentRefreshStrategy.NONE
		);
	}
}
