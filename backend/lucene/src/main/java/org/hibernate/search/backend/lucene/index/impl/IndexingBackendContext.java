/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.lowlevel.directory.impl.DirectoryProvider;
import org.hibernate.search.backend.lucene.lowlevel.writer.impl.IndexWriterHolder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneBatchingWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestratorImplementor;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkProcessor;
import org.hibernate.search.engine.backend.index.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.index.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexWorkPlan;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.multitenancy.impl.MultiTenancyStrategy;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.work.impl.LuceneWorkFactory;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.SuppressingCloser;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.search.util.common.reporting.EventContext;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;

public class IndexingBackendContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final EventContext eventContext;

	private final DirectoryProvider directoryProvider;
	private final LuceneWorkFactory workFactory;
	private final MultiTenancyStrategy multiTenancyStrategy;
	private final ErrorHandler errorHandler;

	public IndexingBackendContext(EventContext eventContext,
			DirectoryProvider directoryProvider,
			LuceneWorkFactory workFactory,
			MultiTenancyStrategy multiTenancyStrategy,
			ErrorHandler errorHandler) {
		this.eventContext = eventContext;
		this.directoryProvider = directoryProvider;
		this.multiTenancyStrategy = multiTenancyStrategy;
		this.workFactory = workFactory;
		this.errorHandler = errorHandler;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + eventContext + "]";
	}

	EventContext getEventContext() {
		return eventContext;
	}

	IndexWriterHolder createIndexWriterHolder(String indexName, Analyzer analyzer) {
		Directory directory;
		try {
			directory = directoryProvider.createDirectory( indexName );
		}
		catch (IOException | RuntimeException e) {
			throw log.unableToCreateIndexDirectory(
					eventContext.append( EventContexts.fromIndexName( indexName ) ),
					e
			);
		}
		try {
			return new IndexWriterHolder( indexName, directory, analyzer, errorHandler );
		}
		catch (RuntimeException e) {
			new SuppressingCloser( e ).push( directory );
			throw e;
		}
	}

	IndexWorkPlan<LuceneRootDocumentBuilder> createWorkPlan(
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

	LuceneWriteWorkOrchestratorImplementor createOrchestrator(String indexName, IndexWriterHolder indexWriterHolder) {
		return new LuceneBatchingWriteWorkOrchestrator(
				"Lucene write work orchestrator for index " + indexName,
				new LuceneWriteWorkProcessor( EventContexts.fromIndexName( indexName ), indexWriterHolder, errorHandler ),
				errorHandler
		);
	}

	IndexDocumentWorkExecutor<LuceneRootDocumentBuilder> createDocumentWorkExecutor(
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

	IndexWorkExecutor createWorkExecutor(LuceneWriteWorkOrchestrator orchestrator, String indexName) {
		return new LuceneIndexWorkExecutor( workFactory, multiTenancyStrategy, orchestrator, indexName, eventContext );
	}
}
