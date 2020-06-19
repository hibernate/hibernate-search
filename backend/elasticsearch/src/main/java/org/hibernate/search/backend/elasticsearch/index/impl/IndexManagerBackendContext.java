/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchBatchingWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchParallelWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.schema.management.impl.ElasticsearchIndexLifecycleExecutionOptions;
import org.hibernate.search.backend.elasticsearch.index.layout.IndexLayoutStrategy;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.document.model.lowlevel.impl.LowLevelIndexMetadataBuilder;
import org.hibernate.search.backend.elasticsearch.schema.management.impl.ElasticsearchIndexSchemaManager;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.backend.elasticsearch.mapping.impl.TypeNameMapping;
import org.hibernate.search.backend.elasticsearch.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchSerialWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchContext;
import org.hibernate.search.backend.elasticsearch.search.impl.ElasticsearchSearchIndexesContext;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.ElasticsearchSearchProjection;
import org.hibernate.search.backend.elasticsearch.search.projection.impl.SearchProjectionBackendContext;
import org.hibernate.search.backend.elasticsearch.search.query.impl.ElasticsearchSearchQueryBuilder;
import org.hibernate.search.backend.elasticsearch.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexIndexer;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexIndexingPlan;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.ElasticsearchIndexWorkspace;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionBackendContext;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.common.spi.EntityReferenceFactory;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
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
	private final ElasticsearchParallelWorkOrchestrator generalPurposeOrchestrator;

	private final SearchProjectionBackendContext searchProjectionBackendContext;

	public IndexManagerBackendContext(ElasticsearchBackend backendAPI,
			EventContext eventContext,
			BackendThreads threads, ElasticsearchLink link, Gson userFacingGson,
			MultiTenancyStrategy multiTenancyStrategy,
			IndexLayoutStrategy indexLayoutStrategy,
			TypeNameMapping typeNameMapping,
			FailureHandler failureHandler,
			ElasticsearchParallelWorkOrchestrator generalPurposeOrchestrator) {
		this.backendAPI = backendAPI;
		this.eventContext = eventContext;
		this.threads = threads;
		this.link = link;
		this.userFacingGson = userFacingGson;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.indexLayoutStrategy = indexLayoutStrategy;
		this.failureHandler = failureHandler;
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
	public <R> IndexIndexingPlan<R> createIndexingPlan(
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext, EntityReferenceFactory<R> entityReferenceFactory,
			DocumentRefreshStrategy refreshStrategy) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new ElasticsearchIndexIndexingPlan<>(
				link.getWorkBuilderFactory(), orchestrator,
				indexManagerContext,
				sessionContext,
				entityReferenceFactory,
				refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(
			ElasticsearchSerialWorkOrchestrator orchestrator,
			WorkExecutionIndexManagerContext indexManagerContext,
			BackendSessionContext sessionContext) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new ElasticsearchIndexIndexer( link.getWorkBuilderFactory(), orchestrator,
				indexManagerContext, sessionContext
		);
	}

	@Override
	public IndexWorkspace createWorkspace(WorkExecutionIndexManagerContext indexManagerContext,
			DetachedBackendSessionContext sessionContext) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new ElasticsearchIndexWorkspace(
				link.getWorkBuilderFactory(), multiTenancyStrategy, generalPurposeOrchestrator,
				indexManagerContext, sessionContext
		);
	}

	@Override
	public SearchProjectionBackendContext getSearchProjectionBackendContext() {
		return searchProjectionBackendContext;
	}

	@Override
	public ElasticsearchSearchContext createSearchContext(BackendMappingContext mappingContext,
			ElasticsearchSearchIndexesContext indexes) {
		return new ElasticsearchSearchContext(
				mappingContext,
				userFacingGson, link.getSearchSyntax(),
				multiTenancyStrategy,
				indexes
		);
	}

	@Override
	public <H> ElasticsearchSearchQueryBuilder<H> createSearchQueryBuilder(
			ElasticsearchSearchContext searchContext,
			BackendSessionContext sessionContext,
			LoadingContextBuilder<?, ?, ?> loadingContextBuilder,
			ElasticsearchSearchProjection<?, H> rootProjection) {
		multiTenancyStrategy.documentIdHelper().checkTenantId( sessionContext.tenantIdentifier(), eventContext );
		return new ElasticsearchSearchQueryBuilder<>(
				link.getWorkBuilderFactory(), link.getSearchResultExtractorFactory(),
				generalPurposeOrchestrator,
				searchContext, sessionContext, loadingContextBuilder, rootProjection
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
				link.getIndexMetadataSyntax(),
				model.getNames()
		);
		model.contributeLowLevelMetadata( builder );
		IndexMetadata expectedMetadata = builder.build();
		return new ElasticsearchIndexSchemaManager(
				link.getWorkBuilderFactory(), generalPurposeOrchestrator,
				indexLayoutStrategy, model.getNames(), expectedMetadata,
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
