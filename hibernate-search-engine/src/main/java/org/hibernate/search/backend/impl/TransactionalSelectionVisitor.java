/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2011, Red Hat, Inc. and/or its affiliates or third-party contributors as
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
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This visitor applies the selection logic from the plugged IndexShardingStrategies to
 * transactional operations, so similar to StreamingSelectionVisitor but preparing a
 * context bound list of operations instead of sending all changes directly to the backend.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class TransactionalSelectionVisitor implements WorkVisitor<ContextAwareSelectionDelegate> {
	
	public static final TransactionalSelectionVisitor INSTANCE = new TransactionalSelectionVisitor();
	
	private final AddSelectionDelegate addDelegate = new AddSelectionDelegate();
	private final DeleteSelectionDelegate deleteDelegate = new DeleteSelectionDelegate();
	private final OptimizeSelectionDelegate optimizeDelegate = new OptimizeSelectionDelegate();
	private final PurgeAllSelectionDelegate purgeDelegate = new PurgeAllSelectionDelegate();
	private final FlushSelectionDelegate flushDelegate = new FlushSelectionDelegate();
	
	private TransactionalSelectionVisitor() {
		// use INSTANCE as this delegator is stateless
	}

	public ContextAwareSelectionDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}
	
	public ContextAwareSelectionDelegate getDelegate(UpdateLuceneWork addLuceneWork) {
		return addDelegate;
	}

	public ContextAwareSelectionDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	public ContextAwareSelectionDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return optimizeDelegate;
	}

	public ContextAwareSelectionDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeDelegate;
	}

	public ContextAwareSelectionDelegate getDelegate(FlushLuceneWork flushLuceneWork) {
		return flushDelegate;
	}

	private static class AddSelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager indexManager = shardingStrategy.getIndexManagerForAddition(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString(),
					work.getDocument()
			);
			context.getIndexManagerQueue( indexManager ).add( work );
		}

	}
	
	private static class DeleteSelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for (IndexManager indexManager : indexManagers) {
				context.getIndexManagerQueue( indexManager ).add( work );
			}
		}

	}

	/**
	 * In this exceptional case we still delegate to a streaming operation instead:
	 * no need for transactions as optimizing doesn't affect index-encoded state.
	 */
	private static class OptimizeSelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work, null, false );
			}
		}

	}

	private static class FlushSelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for (IndexManager indexManager : indexManagers) {
				indexManager.performStreamOperation( work, null, false );
			}
		}

	}

	private static class PurgeAllSelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for (IndexManager indexManager : indexManagers) {
				context.getIndexManagerQueue( indexManager ).add( work );
			}
		}

	}

}
