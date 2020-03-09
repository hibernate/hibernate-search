/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.execution.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentReferenceProvider;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;

import com.google.gson.JsonObject;

public class ElasticsearchIndexIndexer implements IndexIndexer {

	private final ElasticsearchWorkBuilderFactory factory;
	private final ElasticsearchWorkOrchestrator orchestrator;
	private final WorkExecutionIndexManagerContext indexManagerContext;
	private final String tenantId;

	public ElasticsearchIndexIndexer(ElasticsearchWorkBuilderFactory factory,
			ElasticsearchWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext) {
		this.factory = factory;
		this.orchestrator = orchestrator;
		this.indexManagerContext = indexManagerContext;
		this.tenantId = sessionContext.getTenantIdentifier();
	}

	@Override
	public CompletableFuture<?> add(DocumentReferenceProvider referenceProvider,
			DocumentContributor documentContributor) {
		String id = referenceProvider.getIdentifier();
		String elasticsearchId = indexManagerContext.toElasticsearchId( tenantId, id );
		String routingKey = referenceProvider.getRoutingKey();

		JsonObject document = indexManagerContext.createDocument( tenantId, id, documentContributor );

		ElasticsearchWork<Void> work = factory.index(
				indexManagerContext.getMappedTypeName(), referenceProvider.getEntityIdentifier(),
				indexManagerContext.getElasticsearchIndexWriteName(),
				URLEncodedString.fromString( elasticsearchId ), routingKey, document
		)
				.build();
		return orchestrator.submit( work );
	}
}
