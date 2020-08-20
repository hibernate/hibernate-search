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
import org.hibernate.search.backend.impl.StreamingOperationDispatcher;
import org.hibernate.search.backend.impl.TransactionalOperationDispatcher;
import org.hibernate.search.backend.spi.BatchBackend;
import org.hibernate.search.backend.spi.OperationDispatcher;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.IndexedTypeIdentifier;
import org.hibernate.search.spi.IndexedTypeSet;
import org.hibernate.search.spi.IndexedTypeMap;

/**
 * This is not meant to be used as a regular
 * backend, only to apply batch changes to the index. Several threads
 * are used to make changes to each index, so order of Work processing is not guaranteed.
 *
 * @author Sanne Grinovero
 * @hsearch.experimental First {@code BatchBackend}
 */
public class DefaultBatchBackend implements BatchBackend {

	private final ExtendedSearchIntegrator integrator;
	private final MassIndexerProgressMonitor progressMonitor;
	private final OperationDispatcher streamingDispatcher;
	private final OperationDispatcher transactionalDispatcher;

	public DefaultBatchBackend(ExtendedSearchIntegrator integrator, MassIndexerProgressMonitor progressMonitor) {
		this.integrator = integrator;
		this.progressMonitor = progressMonitor;
		this.streamingDispatcher = new StreamingOperationDispatcher( integrator, true /* forceAsync */ );
		this.transactionalDispatcher = new TransactionalOperationDispatcher( integrator );
	}

	@Override
	public void enqueueAsyncWork(LuceneWork work) throws InterruptedException {
		streamingDispatcher.dispatch( work, progressMonitor );
	}

	@Override
	public void awaitAsyncProcessingCompletion() {
		IndexedTypeMap<EntityIndexBinding> indexBindings = integrator.getIndexBindings();
		for ( EntityIndexBinding indexBinding : indexBindings.values() ) {
			for ( IndexManager indexManager : indexBinding.getIndexManagerSelector().all() ) {
				indexManager.awaitAsyncProcessingCompletion();
			}
		}
	}

	@Override
	public void doWorkInSync(LuceneWork work) {
		//FIXME I need a "Force sync" actually for when using PurgeAll before the indexing starts
		transactionalDispatcher.dispatch( work, progressMonitor );
	}

	@Override
	public void flush(IndexedTypeSet entityTypes) {
		Collection<IndexManager> uniqueIndexManagers = uniqueIndexManagerForTypes( entityTypes );
		for ( IndexManager indexManager : uniqueIndexManagers ) {
			indexManager.performStreamOperation( FlushLuceneWork.INSTANCE, progressMonitor, false );
		}
	}

	@Override
	public void optimize(IndexedTypeSet entityTypes) {
		Collection<IndexManager> uniqueIndexManagers = uniqueIndexManagerForTypes( entityTypes );
		for ( IndexManager indexManager : uniqueIndexManagers ) {
			indexManager.performStreamOperation( OptimizeLuceneWork.INSTANCE, progressMonitor, false );
		}
	}

	private Collection<IndexManager> uniqueIndexManagerForTypes(IndexedTypeSet entityTypes) {
		HashMap<String,IndexManager> uniqueBackends = new HashMap<String, IndexManager>( entityTypes.size() );
		for ( IndexedTypeIdentifier type : entityTypes ) {
			EntityIndexBinding indexBindingForEntity = integrator.getIndexBinding( type );
			if ( indexBindingForEntity != null ) {
				Set<IndexManager> indexManagers = indexBindingForEntity.getIndexManagerSelector().all();
				for ( IndexManager im : indexManagers ) {
					uniqueBackends.put( im.getIndexName(), im );
				}
			}
		}
		return uniqueBackends.values();
	}

}
