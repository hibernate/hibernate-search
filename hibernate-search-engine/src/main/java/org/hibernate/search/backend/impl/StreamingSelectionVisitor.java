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
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This visitor applies the selection logic from the plugged IndexShardingStrategies to
 * stream operations, as used by optimize() and batching operations.
 * Using a visitor/selector pattern for different implementations of addAsPayLoadsToQueue
 * depending on the type of LuceneWork.
 * 
 * @author Sanne Grinovero
 */
public class StreamingSelectionVisitor implements WorkVisitor<StreamingOperationSelectionDelegate> {
	
	private final AddSelectionDelegate addDelegate = new AddSelectionDelegate();
	private final DeleteSelectionDelegate deleteDelegate = new DeleteSelectionDelegate();
	private final AllSelectionDelegate allManagersDelegate = new AllSelectionDelegate();
	private final PurgeAllSelectionDelegate purgeDelegate = new PurgeAllSelectionDelegate();
	
	public static final StreamingSelectionVisitor INSTANCE = new StreamingSelectionVisitor();
	
	private StreamingSelectionVisitor() {
		// use INSTANCE as this delegator is stateless
	}

	public StreamingOperationSelectionDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}
	
	public StreamingOperationSelectionDelegate getDelegate(UpdateLuceneWork addLuceneWork) {
		return addDelegate;
	}

	public StreamingOperationSelectionDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	public StreamingOperationSelectionDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return allManagersDelegate;
	}

	public StreamingOperationSelectionDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeDelegate;
	}

	public StreamingOperationSelectionDelegate getDelegate(FlushLuceneWork flushLuceneWork) {
		return allManagersDelegate;
	}

	private static class AddSelectionDelegate implements StreamingOperationSelectionDelegate {

		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager indexManager = shardingStrategy.getIndexManagerForAddition(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString(),
					work.getDocument()
			);
			indexManager.performStreamOperation( work, monitor, forceAsync );
		}

	}
	
	private static class DeleteSelectionDelegate implements StreamingOperationSelectionDelegate {

		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}
	
	private static class AllSelectionDelegate implements StreamingOperationSelectionDelegate {

		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

	private static class PurgeAllSelectionDelegate implements StreamingOperationSelectionDelegate {

		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

}
