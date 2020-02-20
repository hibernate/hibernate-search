/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.lowlevel.query.impl.Queries;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ElasticsearchIndexWorkspace implements IndexWorkspace {

	private final ElasticsearchWorkBuilderFactory builderFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchWorkOrchestrator orchestrator;
	private final URLEncodedString indexName;
	private final DetachedBackendSessionContext sessionContext;

	public ElasticsearchIndexWorkspace(ElasticsearchWorkBuilderFactory builderFactory,
			MultiTenancyStrategy multiTenancyStrategy, ElasticsearchWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			DetachedBackendSessionContext sessionContext) {
		this.builderFactory = builderFactory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestrator = orchestrator;
		this.indexName = indexManagerContext.getElasticsearchIndexWriteName();
		this.sessionContext = sessionContext;
	}

	@Override
	public CompletableFuture<?> mergeSegments() {
		return orchestrator.submit( builderFactory.mergeSegments().index( indexName ).build() );
	}

	@Override
	public CompletableFuture<?> purge() {
		JsonArray filters = new JsonArray();
		JsonObject filter = multiTenancyStrategy.getFilterOrNull( sessionContext.getTenantIdentifier() );
		if ( filter != null ) {
			filters.add( filter );
		}

		JsonObject payload = new JsonObject();
		payload.add(
				"query",
				Queries.boolFilter( Queries.matchAll(), filters )
		);

		return orchestrator.submit( builderFactory.deleteByQuery( indexName, payload ).build() );
	}

	@Override
	public CompletableFuture<?> flush() {
		return orchestrator.submit( builderFactory.flush().index( indexName ).build() );
	}

	@Override
	public CompletableFuture<?> refresh() {
		return orchestrator.submit( builderFactory.refresh().index( indexName ).build() );
	}
}
