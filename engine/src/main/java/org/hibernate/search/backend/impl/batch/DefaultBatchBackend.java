/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.batch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.impl.StreamingOperationExecutor;
import org.hibernate.search.backend.impl.StreamingOperationExecutorSelector;
import org.hibernate.search.backend.impl.TransactionalOperationExecutor;
import org.hibernate.search.backend.impl.TransactionalOperationExecutorSelector;
import org.hibernate.search.backend.impl.WorkQueuePerIndexSplitter;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This is not meant to be used as a regular
 * backend, only to apply batch changes to the index. Several threads
 * are used to make changes to each index, so order of Work processing is not guaranteed.
 *
 * @author Sanne Grinovero
 * @hsearch.experimental First {@code BatchBackend}
 */
public class DefaultBatchBackend implements BatchBackend {

	private final SearchIntegrator integrator;
	private final MassIndexerProgressMonitor progressMonitor;

	public DefaultBatchBackend(SearchIntegrator integrator, MassIndexerProgressMonitor progressMonitor) {
		this.integrator = integrator;
		this.progressMonitor = progressMonitor;
	}

	@Override
	public void enqueueAsyncWork(LuceneWork work) throws InterruptedException {
		sendWorkToShards( work, true );
	}

	@Override
	public void doWorkInSync(LuceneWork work) {
		sendWorkToShards( work, false );
	}

	private void sendWorkToShards(LuceneWork work, boolean forceAsync) {
		final Class<?> entityType = work.getEntityClass();
		EntityIndexBinding entityIndexBinding = integrator.getIndexBinding( entityType );
		IndexShardingStrategy shardingStrategy = entityIndexBinding.getSelectionStrategy();
		if ( forceAsync ) {
			StreamingOperationExecutor executor = work.acceptIndexWorkVisitor( StreamingOperationExecutorSelector.INSTANCE, null );
			executor.performStreamOperation( work, shardingStrategy, progressMonitor, forceAsync );
		}
		else {
			WorkQueuePerIndexSplitter workContext = new WorkQueuePerIndexSplitter();
			TransactionalOperationExecutor executor = work.acceptIndexWorkVisitor( TransactionalOperationExecutorSelector.INSTANCE, null );
			executor.performOperation( work, shardingStrategy, workContext );
			workContext.commitOperations( progressMonitor ); //FIXME I need a "Force sync" actually for when using PurgeAll before the indexing starts
		}
	}

	@Override
	public void flush(Set<Class<?>> entityTypes) {
		Collection<IndexManager> uniqueIndexManagers = uniqueIndexManagerForTypes( entityTypes );
		for ( IndexManager indexManager : uniqueIndexManagers ) {
			indexManager.performStreamOperation( FlushLuceneWork.INSTANCE, progressMonitor, false );
		}
	}

	@Override
	public void optimize(Set<Class<?>> entityTypes) {
		Collection<IndexManager> uniqueIndexManagers = uniqueIndexManagerForTypes( entityTypes );
		for ( IndexManager indexManager : uniqueIndexManagers ) {
			indexManager.performStreamOperation( OptimizeLuceneWork.INSTANCE, progressMonitor, false );
		}
	}

	private Collection<IndexManager> uniqueIndexManagerForTypes(Collection<Class<?>> entityTypes) {
		HashMap<String,IndexManager> uniqueBackends = new HashMap<String, IndexManager>( entityTypes.size() );
		for ( Class<?> type : entityTypes ) {
			EntityIndexBinding indexBindingForEntity = integrator.getIndexBinding( type );
			if ( indexBindingForEntity != null ) {
				IndexManager[] indexManagers = indexBindingForEntity.getIndexManagers();
				for ( IndexManager im : indexManagers ) {
					uniqueBackends.put( im.getIndexName(), im );
				}
			}
		}
		return uniqueBackends.values();
	}

}
