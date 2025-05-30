/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.index.impl;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.backend.elasticsearch.ElasticsearchBackend;
import org.hibernate.search.backend.elasticsearch.analysis.impl.ElasticsearchAnalysisPerformer;
import org.hibernate.search.backend.elasticsearch.document.impl.DocumentMetadataContributor;
import org.hibernate.search.backend.elasticsearch.document.impl.ElasticsearchDocumentObjectBuilder;
import org.hibernate.search.backend.elasticsearch.document.model.impl.ElasticsearchIndexModel;
import org.hibernate.search.backend.elasticsearch.index.ElasticsearchIndexManager;
import org.hibernate.search.backend.elasticsearch.logging.impl.DeprecationLog;
import org.hibernate.search.backend.elasticsearch.logging.impl.ElasticsearchMiscLog;
import org.hibernate.search.backend.elasticsearch.logging.impl.QueryLog;
import org.hibernate.search.backend.elasticsearch.metamodel.ElasticsearchIndexDescriptor;
import org.hibernate.search.backend.elasticsearch.orchestration.impl.ElasticsearchBatchingWorkOrchestrator;
import org.hibernate.search.backend.elasticsearch.schema.management.impl.ElasticsearchIndexSchemaManager;
import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.backend.elasticsearch.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.DocumentContributor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.spi.OptionalConfigurationProperty;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.reporting.EventContext;

import com.google.gson.JsonObject;

class ElasticsearchIndexManagerImpl
		implements IndexManagerImplementor,
		ElasticsearchIndexManager, WorkExecutionIndexManagerContext {

	private static final OptionalConfigurationProperty<String> OBSOLETE_LIFECYCLE_STRATEGY =
			ConfigurationProperty.forKey( "lifecycle.strategy" )
					.asString()
					.build();

	private final IndexManagerBackendContext backendContext;
	private final List<DocumentMetadataContributor> documentMetadataContributors;
	private final ElasticsearchBatchingWorkOrchestrator indexingOrchestrator;
	private final ElasticsearchIndexModel model;

	private ElasticsearchIndexSchemaManager schemaManager;
	private ElasticsearchAnalysisPerformer analysisPerformer;

	ElasticsearchIndexManagerImpl(IndexManagerBackendContext backendContext,
			ElasticsearchIndexModel model,
			List<DocumentMetadataContributor> documentMetadataContributors) {
		this.backendContext = backendContext;
		this.model = model;
		this.documentMetadataContributors = documentMetadataContributors;
		this.indexingOrchestrator = backendContext.createIndexingOrchestrator( model.hibernateSearchIndexName() );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "hibernateSearchName=" )
				.append( model.hibernateSearchIndexName() )
				.append( "]" )
				.toString();
	}

	@Override
	public void start(IndexManagerStartContext context) {
		try {
			/*
			 * Create the initializer and lifecycle strategy late to allow the behavior to change
			 * after the first phase of bootstrap,
			 * based on runtime data such as runtime user configuration or the detected ES version.
			 * Useful for compile-time boot.
			 */

			model.onStart( backendContext );

			schemaManager = backendContext.createSchemaManager(
					model, context.configurationPropertySource()
			);

			// HSEARCH-3759: the lifecycle strategy is now the schema management strategy, at the mapper level
			OBSOLETE_LIFECYCLE_STRATEGY.getAndMap(
					context.configurationPropertySource(),
					ignored -> {
						throw DeprecationLog.INSTANCE.lifecycleStrategyMovedToMapper();
					}
			);

			indexingOrchestrator.start( context.configurationPropertySource() );

			analysisPerformer = backendContext.createAnalysisPerformer( model );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e )
					.push( ElasticsearchBatchingWorkOrchestrator::stop, indexingOrchestrator );
			throw e;
		}
	}

	@Override
	public CompletableFuture<?> preStop() {
		return indexingOrchestrator.preStop();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ElasticsearchBatchingWorkOrchestrator::stop, indexingOrchestrator );
			schemaManager = null;
		}
	}

	@Override
	public String getMappedTypeName() {
		return model.mappedTypeName();
	}

	@Override
	public URLEncodedString getElasticsearchIndexWriteName() {
		return model.names().write();
	}

	@Override
	public String toElasticsearchId(String tenantId, String id) {
		return backendContext.toElasticsearchId( tenantId, id );
	}

	@Override
	public JsonObject createDocument(String tenantId, String id,
			DocumentContributor documentContributor) {
		ElasticsearchDocumentObjectBuilder builder = new ElasticsearchDocumentObjectBuilder( model );
		documentContributor.contribute( builder );
		JsonObject document = builder.build();

		for ( DocumentMetadataContributor contributor : documentMetadataContributors ) {
			contributor.contribute( document, tenantId, id );
		}

		return document;
	}

	public ElasticsearchIndexModel model() {
		return model;
	}

	@Override
	public IndexSchemaManager schemaManager() {
		return schemaManager;
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		// The commit strategy is ignored, because Elasticsearch always commits changes to its transaction log.
		return backendContext.createIndexingPlan(
				indexingOrchestrator,
				this,
				sessionContext,
				refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(BackendSessionContext sessionContext) {
		return backendContext.createIndexer(
				indexingOrchestrator, this, sessionContext
		);
	}

	@Override
	public IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds) {
		return backendContext.createWorkspace( this, tenantIds );
	}

	@Override
	public <SR> IndexScopeBuilder<SR> createScopeBuilder(BackendMappingContext mappingContext, Class<SR> rootScopeType) {
		return new ElasticsearchIndexScopeBuilder<>(
				backendContext, mappingContext, rootScopeType, this
		);
	}

	@Override
	public void addTo(IndexScopeBuilder<?> builder) {
		if ( builder instanceof ElasticsearchIndexScopeBuilder<?> esBuilder ) {
			esBuilder.add( backendContext, this );
		}
		else {
			throw QueryLog.INSTANCE.cannotMixElasticsearchScopeWithOtherType(
					builder, this, backendContext.getEventContext()
			);
		}
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	public ElasticsearchBackend backend() {
		return backendContext.toAPI();
	}

	@Override
	public ElasticsearchIndexDescriptor descriptor() {
		return model;
	}

	@Override
	public List<? extends AnalysisToken> analyze(String analyzerName, String terms) {
		return Futures.unwrappedExceptionJoin(
				analyzeAsync( analyzerName, terms, OperationSubmitter.blocking() ).toCompletableFuture() );
	}

	@Override
	public AnalysisToken normalize(String normalizerName, String terms) {
		return Futures.unwrappedExceptionJoin(
				normalizeAsync( normalizerName, terms, OperationSubmitter.blocking() ).toCompletableFuture() );
	}

	@Override
	public CompletionStage<List<? extends AnalysisToken>> analyzeAsync(String analyzerName, String terms,
			OperationSubmitter operationSubmitter) {
		return analysisPerformer.analyze( analyzerName, terms, operationSubmitter );
	}

	@Override
	public CompletionStage<AnalysisToken> normalizeAsync(String normalizerName, String terms,
			OperationSubmitter operationSubmitter) {
		return analysisPerformer.normalize( normalizerName, terms, operationSubmitter );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( ElasticsearchIndexManager.class ) ) {
			return (T) this;
		}
		throw ElasticsearchMiscLog.INSTANCE.indexManagerUnwrappingWithUnknownType(
				clazz, ElasticsearchIndexManager.class, getBackendAndIndexEventContext()
		);
	}

	private EventContext getBackendAndIndexEventContext() {
		return backendContext.getEventContext().append(
				EventContexts.fromIndexName( model.hibernateSearchName() )
		);
	}

}
