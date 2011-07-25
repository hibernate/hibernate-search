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

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.impl.DpSelectionDelegate;
import org.hibernate.search.backend.impl.WorkVisitor;
import org.hibernate.search.indexes.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This visitor applies the selection logic from the plugged IndexShardingStrategies to
 * stream operations, as used by optimize() and batching operations.
 * Using a visitor/selector pattern for different implementations of addAsPayLoadsToQueue
 * depending on the type of LuceneWork.
 * 
 * @author Sanne Grinovero
 */
public class StreamingSelectionVisitor implements WorkVisitor<DpSelectionDelegate> {
	
	private final AddSelectionDelegate addDelegate = new AddSelectionDelegate();
	private final DeleteSelectionDelegate deleteDelegate = new DeleteSelectionDelegate();
	private final OptimizeSelectionDelegate optimizeDelegate = new OptimizeSelectionDelegate();
	private final PurgeAllSelectionDelegate purgeDelegate = new PurgeAllSelectionDelegate();

	public DpSelectionDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}
	
	public DpSelectionDelegate getDelegate(UpdateLuceneWork addLuceneWork) {
		return addDelegate;
	}

	public DpSelectionDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	public DpSelectionDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return optimizeDelegate;
	}

	public DpSelectionDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeDelegate;
	}
	
	private static class AddSelectionDelegate implements DpSelectionDelegate {

		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy) {
			IndexManager indexManager = shardingStrategy.getIndexManagersForAddition(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString(),
					work.getDocument()
			);
			indexManager.performStreamOperation( work );
		}

	}
	
	private static class DeleteSelectionDelegate implements DpSelectionDelegate {

		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work );
			}
		}

	}
	
	private static class OptimizeSelectionDelegate implements DpSelectionDelegate {

		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work );
			}
		}

	}
	
	private static class PurgeAllSelectionDelegate implements DpSelectionDelegate {

		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work );
			}
		}

	}

}
