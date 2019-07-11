/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.lowlevel.index.impl.IndexAccessor;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestrator;
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestratorImplementor;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.backend.lucene.work.execution.impl.WorkExecutionIndexManagerContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkPlan;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneIndexManagerImpl
		implements IndexManagerImplementor<LuceneRootDocumentBuilder>, LuceneIndexManager,
		LuceneScopeIndexManagerContext, WorkExecutionIndexManagerContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexManagerBackendContext backendContext;

	private final String indexName;
	private final LuceneIndexModel model;

	private final LuceneWriteWorkOrchestratorImplementor writeOrchestrator;
	private final IndexAccessor indexAccessor;

	LuceneIndexManagerImpl(IndexManagerBackendContext backendContext,
			String indexName, LuceneIndexModel model,
			IndexAccessor indexAccessor) {
		this.backendContext = backendContext;

		this.indexName = indexName;
		this.model = model;

		this.writeOrchestrator = backendContext.createOrchestrator(
				indexName, indexAccessor.getIndexWriterDelegator()
		);
		this.indexAccessor = indexAccessor;
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
	public void start(IndexManagerStartContext context) {
		writeOrchestrator.start();
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( LuceneWriteWorkOrchestratorImplementor::close, writeOrchestrator );
			// Close the index writer after the orchestrators, when we're sure all works have been performed
			closer.push( IndexAccessor::close, indexAccessor );
			closer.push( LuceneIndexModel::close, model );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToShutdownBackend( e, getBackendAndIndexEventContext() );
		}
	}

	@Override
	public String getIndexName() {
		return indexName;
	}

	@Override
	public LuceneWriteWorkOrchestrator getWriteOrchestrator() {
		return writeOrchestrator;
	}

	@Override
	public IndexWorkPlan<LuceneRootDocumentBuilder> createWorkPlan(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return backendContext.createWorkPlan(
				this, sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexDocumentWorkExecutor<LuceneRootDocumentBuilder> createDocumentWorkExecutor(
			SessionContextImplementor sessionContext, DocumentCommitStrategy commitStrategy) {
		return backendContext.createDocumentWorkExecutor(
				this, sessionContext,
				commitStrategy
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedSessionContextImplementor sessionContext) {
		return backendContext.createWorkExecutor( this, sessionContext );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(MappingContextImplementor mappingContext) {
		return new LuceneIndexScopeBuilder(
				backendContext, mappingContext, this
		);
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		if ( ! ( builder instanceof LuceneIndexScopeBuilder ) ) {
			throw log.cannotMixLuceneScopeWithOtherType(
					builder, this, backendContext.getEventContext()
			);
		}

		LuceneIndexScopeBuilder luceneBuilder = (LuceneIndexScopeBuilder) builder;
		luceneBuilder.add( backendContext, this );
	}

	@Override
	public IndexReaderHolder openIndexReader() throws IOException {
		return IndexReaderHolder.of( indexAccessor.openDirectoryIndexReader() );
	}

	@Override
	public IndexManager toAPI() {
		return this;
	}

	@Override
	@SuppressWarnings("unchecked") // Checked using reflection
	public <T> T unwrap(Class<T> clazz) {
		if ( clazz.isAssignableFrom( LuceneIndexManager.class ) ) {
			return (T) this;
		}
		throw log.indexManagerUnwrappingWithUnknownType(
				clazz, LuceneIndexManager.class, getBackendAndIndexEventContext()
		);
	}

	public final IndexAccessor getIndexAccessorForTests() {
		return indexAccessor;
	}

	LuceneIndexModel getModel() {
		return model;
	}

	private EventContext getBackendAndIndexEventContext() {
		return backendContext.getEventContext().append(
				EventContexts.fromIndexName( indexName )
		);
	}
}
