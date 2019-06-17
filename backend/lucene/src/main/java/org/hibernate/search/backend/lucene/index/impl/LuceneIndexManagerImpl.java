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
import org.hibernate.search.backend.lucene.orchestration.impl.LuceneWriteWorkOrchestratorImplementor;
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
import org.hibernate.search.backend.lucene.index.spi.ReaderProvider;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.backend.lucene.search.query.impl.SearchBackendContext;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.mapper.mapping.context.spi.MappingContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.DetachedSessionContextImplementor;
import org.hibernate.search.engine.mapper.session.context.spi.SessionContextImplementor;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

import org.apache.lucene.index.IndexReader;



// TODO HSEARCH-3117 in the end the IndexManager won't implement ReaderProvider as it's far more complex than that
class LuceneIndexManagerImpl
		implements IndexManagerImplementor<LuceneRootDocumentBuilder>, LuceneIndexManager, ReaderProvider {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexingBackendContext indexingBackendContext;
	private final SearchBackendContext searchBackendContext;

	private final String indexName;
	private final LuceneIndexModel model;

	private final LuceneWriteWorkOrchestratorImplementor writeOrchestrator;
	private final IndexAccessor indexAccessor;

	LuceneIndexManagerImpl(IndexingBackendContext indexingBackendContext,
			SearchBackendContext searchBackendContext,
			String indexName, LuceneIndexModel model,
			IndexAccessor indexAccessor) {
		this.indexingBackendContext = indexingBackendContext;
		this.searchBackendContext = searchBackendContext;

		this.indexName = indexName;
		this.model = model;

		this.writeOrchestrator = indexingBackendContext.createOrchestrator(
				indexName, indexAccessor.getIndexWriterDelegator()
		);
		this.indexAccessor = indexAccessor;
	}

	LuceneIndexModel getModel() {
		return model;
	}

	@Override
	public void start(IndexManagerStartContext context) {
		writeOrchestrator.start();
	}

	@Override
	public IndexWorkPlan<LuceneRootDocumentBuilder> createWorkPlan(SessionContextImplementor sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return indexingBackendContext.createWorkPlan(
				writeOrchestrator, indexName, sessionContext,
				commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexDocumentWorkExecutor<LuceneRootDocumentBuilder> createDocumentWorkExecutor(
			SessionContextImplementor sessionContext, DocumentCommitStrategy commitStrategy) {
		return indexingBackendContext.createDocumentWorkExecutor(
				writeOrchestrator, indexName, sessionContext,
				commitStrategy
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedSessionContextImplementor sessionContext) {
		return indexingBackendContext.createWorkExecutor( writeOrchestrator, indexName, sessionContext );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(MappingContextImplementor mappingContext) {
		return new LuceneIndexScopeBuilder( searchBackendContext, mappingContext, this );
	}

	@Override
	public void addTo(IndexScopeBuilder builder) {
		if ( ! ( builder instanceof LuceneIndexScopeBuilder ) ) {
			throw log.cannotMixLuceneScopeWithOtherType(
					builder, this, searchBackendContext.getEventContext()
			);
		}

		LuceneIndexScopeBuilder luceneBuilder = (LuceneIndexScopeBuilder) builder;
		luceneBuilder.add( searchBackendContext, this );
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

	ReaderProvider getReaderProvider() {
		return this;
	}

	@Override
	public IndexReader openIndexReader() {
		try {
			return indexAccessor.openDirectoryIndexReader();
		}
		catch (IOException e) {
			throw log.unableToCreateIndexReader( getBackendAndIndexEventContext(), e );
		}
	}

	@Override
	public void closeIndexReader(IndexReader reader) {
		try {
			reader.close();
		}
		catch (IOException e) {
			log.unableToCloseIndexReader( getBackendAndIndexEventContext(), e );
		}
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

	private EventContext getBackendAndIndexEventContext() {
		return indexingBackendContext.getEventContext().append(
				EventContexts.fromIndexName( indexName )
		);
	}
}
