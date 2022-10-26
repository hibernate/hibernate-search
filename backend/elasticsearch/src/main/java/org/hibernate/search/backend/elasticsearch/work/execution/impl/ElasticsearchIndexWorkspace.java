/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.factory.impl.ElasticsearchWorkFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchIndexWorkspace implements IndexWorkspace {

	private final ElasticsearchWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchParallelWorkOrchestrator orchestrator;
	private final URLEncodedString indexName;
	private final DetachedBackendSessionContext sessionContext;

	public ElasticsearchIndexWorkspace(ElasticsearchWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy, ElasticsearchParallelWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			DetachedBackendSessionContext sessionContext) {
		this.workFactory = workFactory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexManagerContext.getElasticsearchIndexWriteName();
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> mergeSegments(OperationSubmitter operationSubmitter) {
		return orchestrator.submit( workFactory.mergeSegments().index( indexName ).build(), operationSubmitter );
	}

	@Override
	public CompletableFuture<?> purge(Set<String> routingKeys, OperationSubmitter operationSubmitter) {
		JsonArray filters = new JsonArray();
		JsonObject filter = multiTenancyStrategy.filterOrNull( sessionContext.tenantIdentifier() );
		if ( filter != null ) {
			filters.add( filter );
		}
		if ( !routingKeys.isEmpty() ) {
			filters.add( Queries.anyTerm( "_routing", routingKeys ) );
		}

		JsonObject payload = new JsonObject();
		payload.add(
				"query",
				Queries.boolFilter( Queries.matchAll(), filters )
		);

		return orchestrator.submit(
				workFactory.deleteByQuery( indexName, payload )
						.routingKeys( routingKeys )
						.build(),
				operationSubmitter
		);
	}

	@Override
	public CompletableFuture<?> flush(OperationSubmitter operationSubmitter) {
		return orchestrator.submit( workFactory.flush().index( indexName ).build(), operationSubmitter );
	}

	@Override
	public CompletableFuture<?> refresh(OperationSubmitter operationSubmitter) {
		return orchestrator.submit( workFactory.refresh().index( indexName ).build(), operationSubmitter );
	}
}
