/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.hibernate.search.engine.spi.DocumentBuilderIndexedEntity;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.impl.CommonPropertiesParse;
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
		this.indexMetadataIsComplete = CommonPropertiesParse.isIndexMetadataComplete( cfg, context );
	}

	@Override
	public <T> DocumentBuilderIndexedEntity<?> getDocumentBuilder(Class<T> entity) {
		return indexManager.getIndexBindingForEntity( entity ).getDocumentBuilder();
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

	@Override
	public void incrementModificationCounter(int modCount) {
		operations.addAndGet( modCount );
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
