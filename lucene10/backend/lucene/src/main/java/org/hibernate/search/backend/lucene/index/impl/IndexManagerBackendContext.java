/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.util.Set;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.cfg.LuceneIndexSettings;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.index.IOStrategyName;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.DebugIOStrategy;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IOStrategy;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessorImpl;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.NearRealTimeIOStrategy;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterConfigSource;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneBatchedWorkProcessor;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneParallelWorkOrchestratorImpl;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSerialWorkOrchestratorImpl;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSyncWorkOrchestrator;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.backend.lucene.schema.management.impl.LuceneIndexSchemaManager;
import org.hibernate.search.backend.lucene.schema.management.impl.SchemaManagementIndexManagerContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneSearchIndexScopeImpl;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryIndexScope;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.lucene.work.execution.impl.LuceneIndexIndexer;
import org.hibernate.search.backend.lucene.work.execution.impl.LuceneIndexIndexingPlan;
import org.hibernate.search.backend.lucene.work.execution.impl.LuceneIndexWorkspace;
import org.hibernate.search.backend.lucene.work.execution.impl.WorkExecutionBackendContext;
import org.hibernate.search.backend.lucene.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.search.loading.spi.SearchLoadingContextBuilder;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.search.similarities.Similarity;

public class IndexManagerBackendContext implements WorkExecutionBackendContext, SearchBackendContext {

	private static final ConfigurationProperty<IOStrategyName> IO_STRATEGY =
			ConfigurationProperty.forKey( LuceneIndexSettings.IO_STRATEGY )
					.as( IOStrategyName.class, IOStrategyName::of )
					.withDefault( LuceneIndexSettings.Defaults.IO_STRATEGY )
					.build();

	private final LuceneBackend backendAPI;
	private final EventContext eventContext;

	private final BackendThreads threads;
	private final Similarity similarity;
	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final TimingSource timingSource;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final FailureHandler failureHandler;
	private final LuceneSyncWorkOrchestrator readOrchestrator;

	public IndexManagerBackendContext(LuceneBackend backendAPI,
			EventContext eventContext,
			BackendThreads threads,
			Similarity similarity,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			FailureHandler failureHandler,
			LuceneSyncWorkOrchestrator readOrchestrator) {
		this.backendAPI = backendAPI;
		this.eventContext = eventContext;
		this.threads = threads;
		this.similarity = similarity;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.timingSource = timingSource;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.workFactory = workFactory;
		this.failureHandler = failureHandler;
		this.readOrchestrator = readOrchestrator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(
			WorkExecutionIndexManagerContext indexManagerContext,
			LuceneIndexEntryFactory indexEntryFactory,
			BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		multiTenancyStrategy.checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new LuceneIndexIndexingPlan(
				workFactory,
				indexManagerContext,
				indexEntryFactory,
				sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(
			WorkExecutionIndexManagerContext indexManagerContext,
			LuceneIndexEntryFactory indexEntryFactory,
			BackendSessionContext sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new LuceneIndexIndexer(
				workFactory,
				indexEntryFactory,
				indexManagerContext,
				sessionContext
		);
	}

	@Override
	public IndexWorkspace createWorkspace(WorkExecutionIndexManagerContext indexManagerContext,
			Set<String> tenantIds) {
		multiTenancyStrategy.checkTenantId( tenantIds, eventContext );

		return new LuceneIndexWorkspace( workFactory, indexManagerContext, tenantIds );
	}

	@Override
	public LuceneSearchQueryIndexScope<?> createSearchContext(BackendMappingContext mappingContext,
			Set<? extends LuceneScopeIndexManagerContext> indexManagerContexts) {
		return new LuceneSearchIndexScopeImpl( mappingContext, this, analysisDefinitionRegistry,
				multiTenancyStrategy, timingSource, indexManagerContexts );
	}

	@Override
	public <H> LuceneSearchQueryBuilder<H> createSearchQueryBuilder(
			LuceneSearchQueryIndexScope<?> scope,
			BackendSessionContext sessionContext,
			SearchLoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<H> rootProjection) {
		multiTenancyStrategy.checkTenantId( sessionContext.tenantIdentifier(), eventContext );

		return new LuceneSearchQueryBuilder<>(
				workFactory,
				readOrchestrator,
				scope,
				sessionContext,
				loadingContextBuilder,
				rootProjection
		);
	}

	LuceneBackend toAPI() {
		return backendAPI;
	}

	EventContext getEventContext() {
		return eventContext;
	}

	LuceneIndexEntryFactory createLuceneIndexEntryFactory(LuceneIndexModel model) {
		return new LuceneIndexEntryFactory( model, multiTenancyStrategy );
	}

	IOStrategy createIOStrategy(ConfigurationPropertySource propertySource) {
		switch ( IO_STRATEGY.get( propertySource ) ) {
			case DEBUG:
				return DebugIOStrategy.create( threads, failureHandler );
			case NEAR_REAL_TIME:
			default:
				return NearRealTimeIOStrategy.create( propertySource, timingSource, threads, failureHandler );
		}
	}

	LuceneIndexSchemaManager createSchemaManager(String indexName, SchemaManagementIndexManagerContext context) {
		return new LuceneIndexSchemaManager( indexName, workFactory, context );
	}

	IndexAccessorImpl createIndexAccessor(LuceneIndexModel model, EventContext shardEventContext,
			DirectoryHolder directoryHolder, IOStrategy ioStrategy,
			ConfigurationPropertySource propertySource) {
		String indexName = model.hibernateSearchName();
		IndexWriterConfigSource writerConfigSource = IndexWriterConfigSource.create(
				similarity, model.getIndexingAnalyzer(), model.codec(), propertySource, shardEventContext
		);
		return ioStrategy.createIndexAccessor(
				indexName, shardEventContext, directoryHolder, writerConfigSource
		);
	}

	LuceneParallelWorkOrchestratorImpl createIndexManagementOrchestrator(EventContext eventContext,
			IndexAccessorImpl indexAccessor) {
		return new LuceneParallelWorkOrchestratorImpl(
				"Lucene index management orchestrator for " + eventContext.render(),
				eventContext,
				indexAccessor,
				threads
		);
	}

	LuceneSerialWorkOrchestratorImpl createIndexingOrchestrator(EventContext eventContext,
			IndexAccessorImpl indexAccessor) {
		return new LuceneSerialWorkOrchestratorImpl(
				"Lucene indexing orchestrator for " + eventContext.render(),
				new LuceneBatchedWorkProcessor(
						eventContext, indexAccessor
				),
				threads,
				failureHandler
		);
	}
}
