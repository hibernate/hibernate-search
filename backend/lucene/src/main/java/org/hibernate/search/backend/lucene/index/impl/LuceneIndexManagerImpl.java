/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.hibernate.search.backend.lucene.LuceneBackend;
import org.hibernate.search.backend.lucene.analysis.impl.LuceneAnalysisPerformer;
import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.logging.impl.LuceneMiscLog;
import org.hibernate.search.backend.lucene.logging.impl.QueryLog;
import org.hibernate.search.backend.lucene.lowlevel.reader.impl.DirectoryReaderCollector;
import org.hibernate.search.backend.lucene.schema.management.impl.LuceneIndexSchemaManager;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.engine.backend.analysis.AnalysisToken;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.metamodel.IndexDescriptor;
import org.hibernate.search.engine.backend.schema.management.spi.IndexSchemaManager;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexer;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkspace;
import org.hibernate.search.engine.common.resources.spi.SavedState;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.common.spi.SearchIndexIdentifierContext;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.impl.Futures;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;

public class LuceneIndexManagerImpl
		implements IndexManagerImplementor, LuceneIndexManager,
		LuceneScopeIndexManagerContext {

	private static final SavedState.Key<SavedState> SHARD_HOLDER_KEY = SavedState.key( "shard_holder" );

	private final IndexManagerBackendContext backendContext;

	private final String indexName;
	private final LuceneIndexModel model;
	private final LuceneIndexEntryFactory indexEntryFactory;

	private final ShardHolder shardHolder;

	private final LuceneIndexSchemaManager schemaManager;
	private final LuceneAnalysisPerformer analysisPerformer;

	LuceneIndexManagerImpl(IndexManagerBackendContext backendContext,
			String indexName, LuceneIndexModel model, LuceneIndexEntryFactory indexEntryFactory) {
		this.backendContext = backendContext;

		this.indexName = indexName;
		this.model = model;
		this.indexEntryFactory = indexEntryFactory;

		this.shardHolder = new ShardHolder( backendContext, model );
		this.schemaManager = backendContext.createSchemaManager( indexName, shardHolder );
		this.analysisPerformer = new LuceneAnalysisPerformer( backendContext.toAPI() );
	}

	@Override
	public String toString() {
		return new StringBuilder( getClass().getSimpleName() )
				.append( "[" )
				.append( "name=" ).append( indexName )
				.append( "]" )
				.toString();
	}

	@Override
	public SavedState saveForRestart() {
		return SavedState.builder()
				.put( SHARD_HOLDER_KEY, shardHolder.saveForRestart() )
				.build();
	}

	@Override
	public void preStart(IndexManagerStartContext context, SavedState savedState) {
		shardHolder.preStart( context, savedState.get( SHARD_HOLDER_KEY ).orElse( SavedState.empty() ) );
	}

	@Override
	public void start(IndexManagerStartContext context) {
		shardHolder.start( context );
	}

	@Override
	public CompletableFuture<?> preStop() {
		return shardHolder.preStop();
	}

	@Override
	public void stop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ShardHolder::stop, shardHolder );
			closer.push( LuceneIndexModel::close, model );
		}
	}

	@Override
	public IndexSchemaManager schemaManager() {
		return schemaManager;
	}

	@Override
	public IndexIndexingPlan createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return backendContext.createIndexingPlan(
				shardHolder, indexEntryFactory,
				sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexIndexer createIndexer(BackendSessionContext sessionContext) {
		return backendContext.createIndexer(
				shardHolder, indexEntryFactory,
				sessionContext
		);
	}

	@Override
	public IndexWorkspace createWorkspace(BackendMappingContext mappingContext, Set<String> tenantIds) {
		return backendContext.createWorkspace( shardHolder, tenantIds );
	}

	@Override
	public <SR> IndexScopeBuilder<SR> createScopeBuilder(BackendMappingContext mappingContext, Class<SR> rootScopeType) {
		return new LuceneIndexScopeBuilder<>(
				backendContext, mappingContext, rootScopeType, this
		);
	}

	@Override
	public void addTo(IndexScopeBuilder<?> builder) {
		if ( builder instanceof LuceneIndexScopeBuilder<?> luceneBuilder ) {
			luceneBuilder.add( backendContext, this );
		}
		else {
			throw QueryLog.INSTANCE.cannotMixLuceneScopeWithOtherType(
					builder, this, backendContext.getEventContext()
			);
		}
	}

	@Override
	public void openIndexReaders(Set<String> routingKeys, DirectoryReaderCollector readerCollector) throws IOException {
		shardHolder.openIndexReaders( routingKeys, readerCollector );
	}

	@Override
	public LuceneIndexModel model() {
		return model;
	}

	@Override
	public SearchIndexIdentifierContext identifier() {
		return model.identifier();
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	public LuceneBackend backend() {
		return backendContext.toAPI();
	}

	@Override
	public IndexDescriptor descriptor() {
		return model;
	}

	@Override
	public List<? extends AnalysisToken> analyze(String analyzerName, String terms) {
		return analysisPerformer.analyze( analyzerName, terms );
	}

	@Override
	public AnalysisToken normalize(String normalizerName, String terms) {
		return analysisPerformer.normalize( normalizerName, terms );
	}

	@Override
	public CompletionStage<List<? extends AnalysisToken>> analyzeAsync(String analyzerName, String terms,
			OperationSubmitter operationSubmitter) {
		return CompletableFuture.completedFuture( analyze( analyzerName, terms ) );
	}

	@Override
	public CompletionStage<AnalysisToken> normalizeAsync(String normalizerName, String terms,
			OperationSubmitter operationSubmitter) {
		return CompletableFuture.completedFuture( normalize( normalizerName, terms ) );
	}

	@Override
	public Analyzer indexingAnalyzer() {
		return model.getIndexingAnalyzer();
	}

	@Override
	public Analyzer searchAnalyzer() {
		return model.getSearchAnalyzer();
	}

	@Override
	public long computeSizeInBytes() {
		return Futures.unwrappedExceptionJoin( computeSizeInBytesAsync() );
	}

	@Override
	public CompletableFuture<Long> computeSizeInBytesAsync() {
		return computeSizeInBytesAsync( OperationSubmitter.rejecting() );
	}

	@Override
	public CompletableFuture<Long> computeSizeInBytesAsync(OperationSubmitter operationSubmitter) {
		return schemaManager.computeSizeInBytes( operationSubmitter );
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( LuceneIndexManager.class ) ) {
			return (T) this;
		}
		throw LuceneMiscLog.INSTANCE.indexManagerUnwrappingWithUnknownType(
				clazz, LuceneIndexManager.class, getBackendAndIndexEventContext()
		);
	}

	public final List<Shard> getShardsForTests() {
		return shardHolder.shardsForTests();
	}

	private EventContext getBackendAndIndexEventContext() {
		return backendContext.getEventContext().append(
				EventContexts.fromIndexName( indexName )
		);
	}
}
