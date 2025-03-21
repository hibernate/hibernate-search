/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.impl;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.cache.impl.LuceneQueryCachingContext;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexRootBuilder;
import org.hibernate.search.backend.lucene.index.impl.IndexManagerBackendContext;
import org.hibernate.search.backend.lucene.index.impl.LuceneIndexManagerBuilder;
import org.hibernate.search.backend.lucene.logging.impl.LuceneMiscLog;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneSyncWorkOrchestratorImpl;
import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.engine.backend.index.spi.IndexManagerBuilder;
import org.hibernate.search.engine.backend.mapping.spi.BackendMapperContext;
import org.hibernate.search.engine.backend.spi.BackendBuildContext;
import org.hibernate.search.engine.backend.spi.BackendImplementor;
import org.hibernate.search.engine.backend.spi.BackendStartContext;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.common.timing.spi.TimingSource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.similarities.Similarity;

public class LuceneBackendImpl implements BackendImplementor, LuceneBackend {

	private final Optional<String> backendName;

	private final EventContext eventContext;

	private final BackendThreads threads;

	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;

	private final LuceneSyncWorkOrchestratorImpl readOrchestrator;

	private final IndexManagerBackendContext indexManagerBackendContext;

	LuceneBackendImpl(Optional<String> backendName,
			EventContext eventContext,
			BackendThreads threads,
			LuceneWorkFactory workFactory,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			LuceneQueryCachingContext cachingContext,
			MultiTenancyStrategy multiTenancyStrategy,
			TimingSource timingSource,
			FailureHandler failureHandler) {
		this.backendName = backendName;
		this.eventContext = eventContext;
		this.threads = threads;

		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		Similarity similarity = analysisDefinitionRegistry.getSimilarity();

		this.readOrchestrator = new LuceneSyncWorkOrchestratorImpl(
				"Lucene read work orchestrator - " + eventContext.render(), similarity, cachingContext
		);

		this.indexManagerBackendContext = new IndexManagerBackendContext(
				this, eventContext, threads, similarity,
				workFactory, multiTenancyStrategy,
				timingSource, analysisDefinitionRegistry,
				failureHandler,
				readOrchestrator
		);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext.render() + "]";
	}

	@Override
	public void start(BackendStartContext context) {
		threads.onStart( context.configurationPropertySource(), context.beanResolver(), context.threadPoolProvider() );
	}

	@Override
	public CompletableFuture<?> preStop() {
		// Nothing to do
		return CompletableFuture.completedFuture( null );
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( LuceneSyncWorkOrchestratorImpl::stop, readOrchestrator );
			closer.push( BackendThreads::onStop, threads );
		}
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( LuceneBackend.class ) ) {
			return (T) this;
		}
		throw LuceneMiscLog.INSTANCE.backendUnwrappingWithUnknownType(
				clazz, LuceneBackend.class, eventContext
		);
	}

	@Override
	public Optional<String> name() {
		return backendName;
	}

	@Override
	public Backend toAPI() {
		return this;
	}

	@Override
	public Optional<Analyzer> analyzer(String name) {
		return Optional.ofNullable( analysisDefinitionRegistry.getAnalyzerDefinition( name ) );
	}

	@Override
	public Optional<Analyzer> normalizer(String name) {
		return Optional.ofNullable( analysisDefinitionRegistry.getNormalizerDefinition( name ) );
	}

	@Override
	public IndexManagerBuilder createIndexManagerBuilder(
			String indexName, String mappedTypeName, BackendBuildContext context, BackendMapperContext backendMapperContext,
			ConfigurationPropertySource propertySource) {

		LuceneIndexRootBuilder indexRootBuilder = new LuceneIndexRootBuilder(
				EventContexts.fromIndexName( indexName ), backendMapperContext, mappedTypeName, analysisDefinitionRegistry
		);

		/*
		 * We do not normalize index names: directory providers are expected to use the exact given index name,
		 * or a reversible conversion of that name, as an internal key (file names, ...),
		 * and therefore the internal key should stay unique.
		 */
		return new LuceneIndexManagerBuilder(
				indexManagerBackendContext,
				indexName, indexRootBuilder
		);
	}
}
