/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.util.EventContext;

public class ElasticsearchIndexWorkExecutor implements IndexWorkExecutor {

	private final ElasticsearchWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchWorkBuilderFactory workBuilderFactory;
	private final ElasticsearchWorkOrchestrator orchestrator;
	private final URLEncodedString indexName;
	private final EventContext eventContext;

	public ElasticsearchIndexWorkExecutor(ElasticsearchWorkFactory workFactory, MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkBuilderFactory workBuilderFactory, ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, EventContext eventContext) {
		this.workFactory = workFactory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workBuilderFactory = workBuilderFactory;
		this.orchestrator = orchestrator;
		this.indexName = indexName;
		this.eventContext = eventContext;
	}

	@Override
	public CompletableFuture<?> optimize() {
		return orchestrator.submit( workBuilderFactory.optimize().index( indexName ).build() );
	}

	@Override
	public CompletableFuture<?> purge(String tenantId) {
		multiTenancyStrategy.checkTenantId( tenantId, eventContext );
		return orchestrator.submit( workFactory.deleteAll( indexName, tenantId ) );
	}

	@Override
	public CompletableFuture<?> flush() {
		return orchestrator.submit( workFactory.flush( indexName ) );
	}
}
