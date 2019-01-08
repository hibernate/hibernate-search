/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorFactory;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.backend.elasticsearch.work.result.impl.CreateIndexResult;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.EventContext;

public class IndexingBackendContext {

	private final EventContext eventContext;
	private final ElasticsearchWorkBuilderFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchWorkOrchestratorFactory orchestratorFactory;

	public IndexingBackendContext(EventContext eventContext, ElasticsearchWorkBuilderFactory workFactory, MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestratorFactory orchestratorFactory) {
		this.eventContext = eventContext;
		this.workFactory = workFactory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestratorFactory = orchestratorFactory;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	EventContext getEventContext() {
		return eventContext;
	}

	CompletableFuture<?> initializeIndex(ElasticsearchWorkOrchestrator orchestrator, URLEncodedString indexName, URLEncodedString typeName,
			ElasticsearchIndexModel model) {
		ElasticsearchWork<?> dropWork = workFactory.dropIndex( indexName ).ignoreIndexNotFound().build();
		ElasticsearchWork<CreateIndexResult> createWork = workFactory.createIndex( indexName )
				.settings( model.getSettings() )
				.mapping( typeName, model.getMapping() )
				.build();

		return orchestrator.submit( Arrays.asList( dropWork, createWork ) );
	}

	ElasticsearchWorkOrchestrator createWorkPlanOrchestrator(String indexName, boolean refreshAfterWrite) {
		return orchestratorFactory.createNonStreamOrchestrator(
				"Elasticsearch non-stream work orchestrator for index " + indexName, refreshAfterWrite
		);
	}

	ElasticsearchWorkOrchestrator createStreamOrchestrator(String indexName) {
		return orchestratorFactory.createStreamOrchestrator(
				"Elasticsearch stream work orchestrator for index " + indexName
		);
	}

	IndexWorkPlan<ElasticsearchDocumentObjectBuilder> createWorkPlan(
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, URLEncodedString typeName,
			boolean refreshAfterWrite,
			SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexWorkPlan(
				workFactory, multiTenancyStrategy, orchestrator,
				indexName, typeName,
				refreshAfterWrite,
				sessionContext
		);
	}

	IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName, URLEncodedString typeName,
			SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexDocumentWorkExecutor( workFactory, multiTenancyStrategy, orchestrator,
				indexName, typeName, sessionContext );
	}

	IndexWorkExecutor createWorkExecutor(ElasticsearchWorkOrchestrator orchestrator, URLEncodedString indexName) {
		return new ElasticsearchIndexWorkExecutor( workFactory, multiTenancyStrategy, orchestrator, indexName, eventContext );
	}
}
