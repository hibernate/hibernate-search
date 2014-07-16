/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.impl.PropertiesParseHelper;
import org.hibernate.search.indexes.impl.DirectoryBasedIndexManager;
import org.hibernate.search.spi.WorkerBuildContext;
import org.hibernate.search.store.Workspace;
import org.hibernate.search.store.optimization.OptimizerStrategy;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Lucene workspace for an IndexManager
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author Sanne Grinovero
 */
public abstract class AbstractWorkspaceImpl implements Workspace {

	private static final Log log = LoggerFactory.make();

	private final OptimizerStrategy optimizerStrategy;
	private final Set<Class<?>> entitiesInIndexManager;
	private final DirectoryBasedIndexManager indexManager;

	protected final IndexWriterHolder writerHolder;
	private boolean indexMetadataIsComplete;

	/**
	 * Keeps a count of modification operations done on the index.
	 */
	private final AtomicLong operations = new AtomicLong( 0L );

	public AbstractWorkspaceImpl(DirectoryBasedIndexManager indexManager, WorkerBuildContext context, Properties cfg) {
		this.indexManager = indexManager;
		this.optimizerStrategy = indexManager.getOptimizerStrategy();
		this.entitiesInIndexManager = indexManager.getContainedTypes();
		this.writerHolder = new IndexWriterHolder( context.getErrorHandler(), indexManager );
		this.indexMetadataIsComplete = PropertiesParseHelper.isIndexMetadataComplete( cfg, context );
	}

	@Override
	public DocumentBuilderIndexedEntity getDocumentBuilder(Class<?> entity) {
		return indexManager.getIndexBinding( entity ).getDocumentBuilder();
	}

	@Override
	public Analyzer getAnalyzer(String name) {
		return indexManager.getAnalyzer( name );
	}

	@Override
	public void optimizerPhase() {
		optimizerStrategy.addOperationWithinTransactionCount( operations.getAndSet( 0L ) );
		optimizerStrategy.optimize( this );
	}

	@Override
	public void performOptimization(IndexWriter writer) {
		optimizerStrategy.performOptimization( writer );
	}

	protected void incrementModificationCounter() {
		operations.addAndGet( 1 );
	}

	@Override
	public Set<Class<?>> getEntitiesInIndexManager() {
		return entitiesInIndexManager;
	}

	@Override
	public abstract void afterTransactionApplied(boolean someFailureHappened, boolean streaming);

	public void shutDownNow() {
		log.shuttingDownBackend( indexManager.getIndexName() );
		writerHolder.closeIndexWriter();
	}

	@Override
	public IndexWriter getIndexWriter() {
		return writerHolder.getIndexWriter();
	}

	public IndexWriter getIndexWriter(ErrorContextBuilder errorContextBuilder) {
		return writerHolder.getIndexWriter( errorContextBuilder );
	}

	@Override
	public boolean areSingleTermDeletesSafe() {
		return indexMetadataIsComplete && entitiesInIndexManager.size() == 1;
	}

}
