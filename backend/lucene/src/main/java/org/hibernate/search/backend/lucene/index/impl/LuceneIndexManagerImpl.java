/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.index.impl;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexEntryFactory;
import org.hibernate.search.backend.lucene.index.LuceneIndexManager;
import org.hibernate.search.backend.lucene.lowlevel.reader.spi.IndexReaderHolder;
import org.hibernate.search.backend.lucene.scope.model.impl.LuceneScopeIndexManagerContext;
import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.index.IndexManager;
import org.hibernate.search.engine.backend.index.spi.IndexManagerStartContext;
import org.hibernate.search.engine.backend.scope.spi.IndexScopeBuilder;
import org.hibernate.search.engine.backend.work.execution.spi.IndexWorkExecutor;
import org.hibernate.search.engine.backend.index.spi.IndexManagerImplementor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexDocumentWorkExecutor;
import org.hibernate.search.engine.backend.work.execution.spi.IndexIndexingPlan;
import org.hibernate.search.backend.lucene.document.impl.LuceneRootDocumentBuilder;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexModel;
import org.hibernate.search.backend.lucene.logging.impl.Log;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.engine.backend.mapping.spi.BackendMappingContext;
import org.hibernate.search.engine.backend.session.spi.DetachedBackendSessionContext;
import org.hibernate.search.engine.backend.session.spi.BackendSessionContext;
import org.hibernate.search.util.common.reporting.EventContext;
import org.hibernate.search.engine.reporting.spi.EventContexts;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

public class LuceneIndexManagerImpl
		implements IndexManagerImplementor<LuceneRootDocumentBuilder>, LuceneIndexManager,
		LuceneScopeIndexManagerContext {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final IndexManagerBackendContext backendContext;

	private final String indexName;
	private final LuceneIndexModel model;
	private final LuceneIndexEntryFactory indexEntryFactory;

	private final ShardHolder shardHolder;

	LuceneIndexManagerImpl(IndexManagerBackendContext backendContext,
			String indexName, LuceneIndexModel model, LuceneIndexEntryFactory indexEntryFactory) {
		this.backendContext = backendContext;

		this.indexName = indexName;
		this.model = model;
		this.indexEntryFactory = indexEntryFactory;

		this.shardHolder = new ShardHolder( backendContext, model );
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
		shardHolder.start( context );
	}

	@Override
	public void close() {
		try ( Closer<IOException> closer = new Closer<>() ) {
			closer.push( ShardHolder::close, shardHolder );
			closer.push( LuceneIndexModel::close, model );
		}
		catch (IOException | RuntimeException e) {
			throw log.failedToShutdownBackend( e, getBackendAndIndexEventContext() );
		}
	}

	@Override
	public IndexIndexingPlan<LuceneRootDocumentBuilder> createIndexingPlan(BackendSessionContext sessionContext,
			DocumentCommitStrategy commitStrategy, DocumentRefreshStrategy refreshStrategy) {
		return backendContext.createIndexingPlan(
				shardHolder, indexEntryFactory,
				sessionContext, commitStrategy, refreshStrategy
		);
	}

	@Override
	public IndexDocumentWorkExecutor<LuceneRootDocumentBuilder> createDocumentWorkExecutor(
			BackendSessionContext sessionContext, DocumentCommitStrategy commitStrategy) {
		return backendContext.createDocumentWorkExecutor(
				shardHolder, indexEntryFactory,
				sessionContext, commitStrategy
		);
	}

	@Override
	public IndexWorkExecutor createWorkExecutor(DetachedBackendSessionContext sessionContext) {
		return backendContext.createWorkExecutor( shardHolder, sessionContext );
	}

	@Override
	public IndexScopeBuilder createScopeBuilder(BackendMappingContext mappingContext) {
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
	public void openIndexReaders(Set<String> routingKeys, Collection<IndexReaderHolder> readerCollector)
			throws IOException {
		shardHolder.openIndexReaders( routingKeys, readerCollector );
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

	public final List<Shard> getShardsForTests() {
		return shardHolder.getShardsForTests();
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
