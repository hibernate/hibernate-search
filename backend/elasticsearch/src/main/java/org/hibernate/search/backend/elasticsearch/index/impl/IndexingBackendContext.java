/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.work.builder.factory.impl.ElasticsearchWorkBuilderFactory;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorProvider;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.EventContext;

public class IndexingBackendContext {

	private final EventContext eventContext;
	private final ElasticsearchWorkBuilderFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchWorkOrchestratorProvider orchestratorProvider;

	public IndexingBackendContext(EventContext eventContext, ElasticsearchWorkBuilderFactory workFactory, MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestratorProvider orchestratorProvider) {
		this.eventContext = eventContext;
		this.workFactory = workFactory;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestratorProvider = orchestratorProvider;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	EventContext getEventContext() {
		return eventContext;
	}

	ElasticsearchIndexAdministrationClient createAdministrationClient(URLEncodedString indexName, URLEncodedString typeName,
			ElasticsearchIndexModel model) {
		IndexMetadata metadata = new IndexMetadata();
		metadata.setName( model.getElasticsearchIndexName() );
		metadata.setTypeName( typeName );
		metadata.setSettings( model.getSettings() );
		metadata.setMapping( model.getMapping() );
		return new ElasticsearchIndexAdministrationClient(
				workFactory, orchestratorProvider.getRootParallelOrchestrator(),
				indexName, metadata
		);
	}

	ElasticsearchWorkOrchestrator createSerialOrchestrator(String indexName, boolean refreshAfterWrite) {
		return orchestratorProvider.createSerialOrchestrator(
				"Elasticsearch serial work orchestrator for index " + indexName, refreshAfterWrite
		);
	}

	ElasticsearchWorkOrchestrator createParallelOrchestrator(String indexName) {
		return orchestratorProvider.createParallelOrchestrator(
				"Elasticsearch parallel work orchestrator for index " + indexName
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
