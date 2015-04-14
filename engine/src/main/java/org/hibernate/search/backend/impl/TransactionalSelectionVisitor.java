/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteByQueryLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This visitor applies the selection logic from the plugged IndexShardingStrategies to transactional operations, so
 * similar to StreamingSelectionVisitor but preparing a context bound list of operations instead of sending all changes
 * directly to the backend.
 * <p>
 * Implementation note: This {@link IndexWorkVisitor} implementation intentionally does not perform the actual logic
 * within the individual visit methods themselves but rather returns a delegate class for that purpose. This is to avoid
 * the need for the allocation of a parameter object with the required input data, instead a method with the required
 * parameters is exposed on said delegate.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 * @author Martin Braun
 */
public class TransactionalSelectionVisitor implements IndexWorkVisitor<Void, ContextAwareSelectionDelegate> {

	public static final TransactionalSelectionVisitor INSTANCE = new TransactionalSelectionVisitor();

	private final AddSelectionDelegate addDelegate = new AddSelectionDelegate();
	private final DeleteSelectionDelegate deleteDelegate = new DeleteSelectionDelegate();
	private final OptimizeSelectionDelegate optimizeDelegate = new OptimizeSelectionDelegate();
	private final PurgeAllSelectionDelegate purgeDelegate = new PurgeAllSelectionDelegate();
	private final FlushSelectionDelegate flushDelegate = new FlushSelectionDelegate();
	private final DeleteByQuerySelectionDelegate deleteByQueryDelegate = new DeleteByQuerySelectionDelegate();

	private TransactionalSelectionVisitor() {
		// use INSTANCE as this delegator is stateless
	}

	@Override
	public ContextAwareSelectionDelegate visitAddWork(AddLuceneWork addLuceneWork, Void p) {
		return addDelegate;
	}

	@Override
	public ContextAwareSelectionDelegate visitUpdateWork(UpdateLuceneWork updateLuceneWork, Void p) {
		return addDelegate;
	}

	@Override
	public ContextAwareSelectionDelegate visitDeleteWork(DeleteLuceneWork deleteLuceneWork, Void p) {
		return deleteDelegate;
	}

	@Override
	public ContextAwareSelectionDelegate visitOptimizeWork(OptimizeLuceneWork optimizeLuceneWork, Void p) {
		return optimizeDelegate;
	}

	@Override
	public ContextAwareSelectionDelegate visitPurgeAllWork(PurgeAllLuceneWork purgeAllLuceneWork, Void p) {
		return purgeDelegate;
	}

	@Override
	public ContextAwareSelectionDelegate visitFlushWork(FlushLuceneWork flushLuceneWork, Void p) {
		return flushDelegate;
	}

	@Override
	public ContextAwareSelectionDelegate visitDeleteByQueryWork(DeleteByQueryLuceneWork deleteByQueryLuceneWork, Void p) {
		return deleteByQueryDelegate;
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
			for ( IndexManager indexManager : indexManagers ) {
				context.getIndexManagerQueue( indexManager ).add( work );
			}
		}

	}

	private static class DeleteByQuerySelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for ( IndexManager indexManager : indexManagers ) {
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
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, null, false );
			}
		}

	}

	private static class FlushSelectionDelegate implements ContextAwareSelectionDelegate {

		@Override
		public final void performOperation(LuceneWork work, IndexShardingStrategy shardingStrategy,
				WorkQueuePerIndexSplitter context) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for ( IndexManager indexManager : indexManagers ) {
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
			for ( IndexManager indexManager : indexManagers ) {
				context.getIndexManagerQueue( indexManager ).add( work );
			}
		}

	}
}
