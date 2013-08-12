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
package org.hibernate.search.backend.impl.batch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Set;

import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.impl.StreamingSelectionVisitor;
import org.hibernate.search.backend.impl.TransactionalSelectionVisitor;
import org.hibernate.search.backend.impl.WorkQueuePerIndexSplitter;
import org.hibernate.search.batchindexing.MassIndexerProgressMonitor;
import org.hibernate.search.engine.spi.EntityIndexBinding;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.SearchFactoryIntegrator;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This is not meant to be used as a regular
 * backend, only to apply batch changes to the index. Several threads
 * are used to make changes to each index, so order of Work processing is not guaranteed.
 *
 * @author Sanne Grinovero
 * @experimental First {@code BatchBackend}
 */
public class DefaultBatchBackend implements BatchBackend {

	private final SearchFactoryIntegrator searchFactoryImplementor;
	private final MassIndexerProgressMonitor progressMonitor;

	public DefaultBatchBackend(SearchFactoryIntegrator searchFactoryImplementor, MassIndexerProgressMonitor progressMonitor) {
		this.searchFactoryImplementor = searchFactoryImplementor;
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
		EntityIndexBinding entityIndexBinding = searchFactoryImplementor.getIndexBinding( entityType );
		IndexShardingStrategy shardingStrategy = entityIndexBinding.getSelectionStrategy();
		if ( forceAsync ) {
			work.getWorkDelegate( StreamingSelectionVisitor.INSTANCE )
					.performStreamOperation( work, shardingStrategy, progressMonitor, forceAsync );
		}
		else {
			WorkQueuePerIndexSplitter workContext = new WorkQueuePerIndexSplitter();
			work.getWorkDelegate( TransactionalSelectionVisitor.INSTANCE )
					.performOperation( work, shardingStrategy, workContext );
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
			EntityIndexBinding indexBindingForEntity = searchFactoryImplementor.getIndexBinding( type );
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
