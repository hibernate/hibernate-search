/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.analysis.model.impl.LuceneAnalysisDefinitionRegistry;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.DirectoryCreationContextImpl;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryCreationContext;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.directory.spi.DirectoryHolder;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterDelegator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneBatchingWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneReadWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestratorImplementor;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeModel;
import org.hibernate.search.backend.lucene.search.extraction.impl.LuceneDocumentStoredFieldVisitorBuilder;
import org.hibernate.search.backend.lucene.search.impl.LuceneSearchContext;
import org.hibernate.search.backend.lucene.search.projection.impl.LuceneSearchProjection;
import org.hibernate.search.backend.lucene.search.query.impl.LuceneSearchQueryBuilder;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.backend.lucene.work.execution.impl.LuceneIndexDocumentWorkExecutor;
import org.hibernate.search.backend.lucene.work.execution.impl.LuceneIndexWorkExecutor;
import org.hibernate.search.backend.lucene.work.execution.impl.LuceneIndexWorkPlan;
import org.hibernate.search.backend.lucene.work.execution.impl.WorkExecutionBackendContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.engine.search.loading.context.spi.LoadingContextBuilder;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;

public class IndexManagerBackendContext implements WorkExecutionBackendContext, SearchBackendContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;

	private final DirectoryProvider directoryProvider;
	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry;
	private final ErrorHandler errorHandler;
	private final LuceneReadWorkOrchestrator readOrchestrator;

	public IndexManagerBackendContext(EventContext eventContext,
			DirectoryProvider directoryProvider,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			LuceneAnalysisDefinitionRegistry analysisDefinitionRegistry,
			ErrorHandler errorHandler,
			LuceneReadWorkOrchestrator readOrchestrator) {
		this.eventContext = eventContext;
		this.directoryProvider = directoryProvider;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.analysisDefinitionRegistry = analysisDefinitionRegistry;
		this.workFactory = workFactory;
		this.errorHandler = errorHandler;
		this.readOrchestrator = readOrchestrator;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	@Override
	public IndexWorkPlan<LuceneRootDocumentBuilder> createWorkPlan(
			LuceneWriteWorkOrchestrator orchestrator,
			String indexName, SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new LuceneIndexWorkPlan(
				workFactory, multiTenancyStrategy, orchestrator,
				indexName, sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public LuceneWriteWorkOrchestratorImplementor createOrchestrator(String indexName,
			IndexWriterDelegator indexWriterDelegator) {
		return new LuceneBatchingWriteWorkOrchestrator(
				"Lucene write work orchestrator for index " + indexName,
				new LuceneWriteWorkProcessor( EventContexts.fromIndexName( indexName ), indexWriterDelegator, errorHandler ),
				errorHandler
		);
	}

	@Override
	public IndexDocumentWorkExecutor<LuceneRootDocumentBuilder> createDocumentWorkExecutor(
			LuceneWriteWorkOrchestrator orchestrator,
			String indexName, SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new LuceneIndexDocumentWorkExecutor(
				workFactory, multiTenancyStrategy, orchestrator,
				indexName, sessionContext,
				commitStrategy
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(LuceneWriteWorkOrchestrator orchestrator, String indexName,
			DetachedSessionContextImplementor sessionContext) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		return new LuceneIndexWorkExecutor( workFactory, orchestrator, sessionContext );
	}

	@Override
	public LuceneSearchContext createSearchContext(MappingContextImplementor mappingContext,
			LuceneScopeModel scopeModel) {
		return new LuceneSearchContext(
				mappingContext, analysisDefinitionRegistry, multiTenancyStrategy, scopeModel
		);
	}

	@Override
	public <H> LuceneSearchQueryBuilder<H> createSearchQueryBuilder(
			LuceneSearchContext searchContext,
			SessionContextImplementor sessionContext,
			LoadingContextBuilder<?, ?> loadingContextBuilder,
			LuceneSearchProjection<?, H> rootProjection) {
		multiTenancyStrategy.checkTenantId( sessionContext.getTenantIdentifier(), eventContext );

		LuceneDocumentStoredFieldVisitorBuilder storedFieldFilterBuilder = new LuceneDocumentStoredFieldVisitorBuilder();
		rootProjection.contributeFields( storedFieldFilterBuilder );

		return new LuceneSearchQueryBuilder<>(
				workFactory,
				readOrchestrator,
				searchContext,
				sessionContext,
				storedFieldFilterBuilder.build(),
				loadingContextBuilder,
				rootProjection
		);
	}

	EventContext getEventContext() {
		return eventContext;
	}

	IndexAccessor createIndexAccessor(String indexName, Analyzer analyzer) {
		DirectoryHolder directory;
		DirectoryCreationContext context = new DirectoryCreationContextImpl(
				eventContext.append( EventContexts.fromIndexName( indexName ) ),
				indexName
		);
		try {
			directory = directoryProvider.createDirectory( context );
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToInitializeIndexDirectory(
					e.getMessage(),
					context.getEventContext(),
					e
			);
		}
		try {
			return new IndexAccessor( indexName, directory, analyzer, errorHandler );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( directory );
			throw e;
		}
	}

}
