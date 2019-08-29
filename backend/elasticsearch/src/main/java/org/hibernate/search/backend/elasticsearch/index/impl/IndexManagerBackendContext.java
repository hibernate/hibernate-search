/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.function.Function;

import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorImplementor;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchScopeModel;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.DocumentReferenceExtractorHelper;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.ElasticsearchIndexAdministrationClient;
import org.hibernate.search.backend.elasticsearch.index.admin.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchWorkOrchestratorProvider;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexDocumentWorkExecutor;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexWorkExecutor;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexWorkPlan;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionBackendContext;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

public class IndexManagerBackendContext implements SearchBackendContext, WorkExecutionBackendContext {

	private final EventContext eventContext;
	private final ElasticsearchLink link;
	private final Gson userFacingGson;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ElasticsearchWorkOrchestratorProvider orchestratorProvider;
	private final ElasticsearchWorkOrchestrator queryOrchestrator;

	private final SearchProjectionBackendContext searchProjectionBackendContext;
	private final DocumentReferenceExtractorHelper documentReferenceExtractorHelper;

	public IndexManagerBackendContext(EventContext eventContext, ElasticsearchLink link, Gson userFacingGson,
			Function<String, String> indexNameConverter,
			MultiTenancyStrategy multiTenancyStrategy,
			ElasticsearchWorkOrchestratorProvider orchestratorProvider,
			ElasticsearchWorkOrchestrator queryOrchestrator) {
		this.eventContext = eventContext;
		this.link = link;
		this.userFacingGson = userFacingGson;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.orchestratorProvider = orchestratorProvider;
		this.queryOrchestrator = queryOrchestrator;

		this.documentReferenceExtractorHelper =
				new DocumentReferenceExtractorHelper( indexNameConverter, multiTenancyStrategy );
		this.searchProjectionBackendContext = new SearchProjectionBackendContext(
				documentReferenceExtractorHelper,
				userFacingGson
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	@Override
	public IndexWorkPlan<ElasticsearchDocumentObjectBuilder> createWorkPlan(
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName,
			DocumentRefreshStrategy refreshStrategy,
			SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexWorkPlan(
				link.getWorkBuilderFactory(), multiTenancyStrategy, orchestrator,
				indexName,
				refreshStrategy,
				sessionContext
		);
	}

	@Override
	public IndexDocumentWorkExecutor<ElasticsearchDocumentObjectBuilder> createDocumentWorkExecutor(
			ElasticsearchWorkOrchestrator orchestrator,
			URLEncodedString indexName,
			SessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexDocumentWorkExecutor( link.getWorkBuilderFactory(), multiTenancyStrategy, orchestrator,
				indexName, sessionContext );
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(ElasticsearchWorkOrchestrator orchestrator, URLEncodedString indexName,
			DetachedSessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new ElasticsearchIndexWorkExecutor(
				link.getWorkBuilderFactory(), multiTenancyStrategy, orchestrator, indexName,
				sessionContext
		);
	}

	@Override
	public DocumentReferenceExtractorHelper getDocumentReferenceExtractorHelper() {
		return documentReferenceExtractorHelper;
	}

	@Override
	public SearchProjectionBackendContext getSearchProjectionBackendContext() {
		return searchProjectionBackendContext;
	}

	@Override
	public ElasticsearchSearchContext createSearchContext(MappingContextImplementor mappingContext,
			ElasticsearchScopeModel scopeModel) {
		return new ElasticsearchSearchContext(
				mappingContext,
				userFacingGson, link.getJsonSyntaxHelper(),
				multiTenancyStrategy,
				scopeModel
		);
	}

	@Override
	public <H> ElasticsearchSearchQueryBuilder<H> createSearchQueryBuilder(
			ElasticsearchSearchContext searchContext,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, H> rootProjection) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );
		return new ElasticsearchSearchQueryBuilder<>(
				link.getWorkBuilderFactory(), link.getSearchResultExtractorFactory(),
				queryOrchestrator, multiTenancyStrategy,
				searchContext, sessionContext, loadingContextBuilder, rootProjection
		);
	}

	EventContext getEventContext() {
		return eventContext;
	}

	ElasticsearchIndexAdministrationClient createAdministrationClient(URLEncodedString indexName,
			ElasticsearchIndexModel model) {
		IndexMetadata metadata = new IndexMetadata();
		metadata.setName( model.getElasticsearchIndexName() );
		metadata.setSettings( model.getSettings() );
		metadata.setMapping( model.getMapping() );
		return new ElasticsearchIndexAdministrationClient(
				link, orchestratorProvider.getRootParallelOrchestrator(),
				indexName, metadata
		);
	}

	ElasticsearchWorkOrchestratorImplementor createSerialOrchestrator(String indexName) {
		return orchestratorProvider.createSerialOrchestrator(
				"Elasticsearch serial work orchestrator for index " + indexName
		);
	}

	ElasticsearchWorkOrchestratorImplementor createParallelOrchestrator(String indexName) {
		return orchestratorProvider.createParallelOrchestrator(
				"Elasticsearch parallel work orchestrator for index " + indexName
		);
	}
}
