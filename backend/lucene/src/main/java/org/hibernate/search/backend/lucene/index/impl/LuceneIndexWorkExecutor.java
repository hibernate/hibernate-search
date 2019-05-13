/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.util.common.reporting.EventContext;

class LuceneIndexWorkExecutor implements IndexWorkExecutor {

	private final LuceneWorkFactory factory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final LuceneWriteWorkOrchestrator orchestrator;
	private final String indexName;
	private final EventContext eventContext;

	LuceneIndexWorkExecutor(LuceneWorkFactory factory, MultiTenancyStrategy multiTenancyStrategy, LuceneWriteWorkOrchestrator orchestrator, String indexName,
			EventContext eventContext) {
		this.factory = factory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.eventContext = eventContext;
	}

	@Override
	public CompletableFuture<?> optimize() {
		return orchestrator.submit( factory.optimize( indexName ) );
	}

	@Override
	public CompletableFuture<?> purge(String tenantId) {
		multiTenancyStrategy.checkTenantId( tenantId, eventContext );
		return orchestrator.submit( factory.deleteAll( indexName, tenantId ) );
	}

	@Override
	public CompletableFuture<?> flush() {
		return orchestrator.submit( factory.flush( indexName ) );
	}
}
