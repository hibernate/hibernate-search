/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.Set;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl.LowLevelIndexMetadataBuilder;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchBatchingWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.schema.management.impl.ElasticsearchIndexLifecycleExecutionOptions;
import org.hibernate.search.backend.elasticsearch.schema.management.impl.ElasticsearchIndexSchemaManager;
import org.hibernate.search.backend.elasticsearch.scope.model.impl.ElasticsearchSearchIndexScopeImpl;
import org.hibernate.search.backend.elasticsearch.search.common.impl.ElasticsearchSearchIndexScope;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryIndexScope;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexIndexer;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexIndexingPlan;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexWorkspace;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionBackendContext;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.Gson;

public class IndexManagerBackendContext implements SearchBackendContext, WorkExecutionBackendContext {

	private final ElasticsearchBackend backendAPI;
	private final EventContext eventContext;
	private final BackendThreads threads;
	private final ElasticsearchLink link;
	private final Gson userFacingGson;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final IndexLayoutStrategy indexLayoutStrategy;
	private final FailureHandler failureHandler;
	private final TimingSource timingSource;
	private final ElasticsearchParallelWorkOrchestrator generalPurposeOrchestrator;

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	public IndexManagerBackendContext(ElasticsearchBackend backendAPI,
			EventContext eventContext,
			BackendThreads threads, ElasticsearchLink link, Gson userFacingGson,
			MultiTenancyStrategy multiTenancyStrategy,
			IndexLayoutStrategy indexLayoutStrategy,
			TypeNameMapping typeNameMapping,
			FailureHandler failureHandler,
			TimingSource timingSource,
			ElasticsearchParallelWorkOrchestrator generalPurposeOrchestrator) {
		this.backendAPI = backendAPI;
		this.eventContext = eventContext;
		this.threads = threads;
		this.link = link;
		this.userFacingGson = userFacingGson;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.indexLayoutStrategy = indexLayoutStrategy;
		this.failureHandler = failureHandler;
		this.timingSource = timingSource;
		this.generalPurposeOrchestrator = generalPurposeOrchestrator;

		this.searchProjectionBackendContext = new SearchProjectionBackendContext(
				typeNameMapping.getTypeNameExtractionHelper(),
				multiTenancyStrategy.idProjectionExtractionHelper()
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext,
			DocumentRefreshStrategy refreshStrategy) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new ElasticsearchIndexIndexingPlan(
				link.getWorkFactory(), orchestrator,
				indexManagerContext,
				sessionContext,
				refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new ElasticsearchIndexIndexer( link.getWorkFactory(), orchestrator,
				indexManagerContext, sessionContext
		);
	}

	@Override
	public IndexWorkspace createWorkspace(WorkExecutionIndexManagerContext indexManagerContext, Set<String> tenantIds) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( tenantIds, eventContext );

		return new ElasticsearchIndexWorkspace(
				link.getWorkFactory(), multiTenancyStrategy, generalPurposeOrchestrator,
				indexManagerContext, tenantIds
		);
	}

	@Override
	public SearchProjectionBackendContext getSearchProjectionBackendContext() {
		return searchProjectionBackendContext;
	}

	@Override
	public ElasticsearchSearchQueryIndexScope<?> createSearchContext(BackendMappingContext mappingContext,
			Set<ElasticsearchIndexModel> indexModels) {
		return new ElasticsearchSearchIndexScopeImpl(
				mappingContext,
				this,
				userFacingGson, link.getSearchSyntax(),
				multiTenancyStrategy,
				timingSource,
				indexModels
		);
	}

	@Override
	public <H> ElasticsearchSearchQueryBuilder<H> createSearchQueryBuilder(
			ElasticsearchSearchIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<H> rootProjection) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );
		return new ElasticsearchSearchQueryBuilder<>(
				link.getWorkFactory(), link.getSearchResultExtractorFactory(),
				generalPurposeOrchestrator,
				scope, sessionContext, loadingContextBuilder, rootProjection,
				link.getScrollTimeout()
		);
	}

	ElasticsearchBackend toAPI() {
		return backendAPI;
	}

	EventContext getEventContext() {
		return eventContext;
	}

	ElasticsearchIndexSchemaManager createSchemaManager(ElasticsearchIndexModel model,
			ElasticsearchIndexLifecycleExecutionOptions lifecycleExecutionOptions) {
		LowLevelIndexMetadataBuilder builder = new LowLevelIndexMetadataBuilder(
				link.getGsonProvider(),
				link.getIndexMetadataSyntax(),
				model.names()
		);
		model.contributeLowLevelMetadata( builder );
		IndexMetadata expectedMetadata = builder.build();
		return new ElasticsearchIndexSchemaManager(
				backendAPI.name(),
				userFacingGson,
				link.getWorkFactory(), generalPurposeOrchestrator,
				indexLayoutStrategy, model.names(), expectedMetadata,
				lifecycleExecutionOptions
		);
	}

	ElasticsearchBatchingWorkOrchestrator createIndexingOrchestrator(String indexName) {
		return new ElasticsearchBatchingWorkOrchestrator(
				"Elasticsearch indexing orchestrator for index " + indexName,
				threads, link,
				failureHandler
		);
	}

	String toElasticsearchId(String tenantId, String id) {
		return multiTenancyStrategy.documentIdHelper().toElasticsearchId( tenantId, id );
	}

}
