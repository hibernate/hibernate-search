/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;


import java.util.Set;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.indexes.spi.IndexManagerSelector;

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
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 * @author Martin Braun
 */
public class TransactionalOperationExecutorSelector implements IndexWorkVisitor<Void, TransactionalOperationExecutor> {

	public static final TransactionalOperationExecutorSelector INSTANCE = new TransactionalOperationExecutorSelector();

	private final AddSelectionExecutor addExecutor = new AddSelectionExecutor();
	private final DeleteSelectionExecutor deleteExecutor = new DeleteSelectionExecutor();
	private final OptimizeSelectionExecutor optimizeExecutor = new OptimizeSelectionExecutor();
	private final PurgeAllSelectionExecutor purgeExecutor = new PurgeAllSelectionExecutor();
	private final FlushSelectionExecutor flushExecutor = new FlushSelectionExecutor();
	private final DeleteByQuerySelectionExecutor deleteByQueryExecutor = new DeleteByQuerySelectionExecutor();

	private TransactionalOperationExecutorSelector() {
		// Private, use INSTANCE instead
	}

	@Override
	public TransactionalOperationExecutor visitAddWork(AddLuceneWork addLuceneWork, Void p) {
		return addExecutor;
	}

	@Override
	public TransactionalOperationExecutor visitUpdateWork(UpdateLuceneWork updateLuceneWork, Void p) {
		return addExecutor;
	}

	@Override
	public TransactionalOperationExecutor visitDeleteWork(DeleteLuceneWork deleteLuceneWork, Void p) {
		return deleteExecutor;
	}

	@Override
	public TransactionalOperationExecutor visitOptimizeWork(OptimizeLuceneWork optimizeLuceneWork, Void p) {
		return optimizeExecutor;
	}

	@Override
	public TransactionalOperationExecutor visitPurgeAllWork(PurgeAllLuceneWork purgeAllLuceneWork, Void p) {
		return purgeExecutor;
	}

	@Override
	public TransactionalOperationExecutor visitFlushWork(FlushLuceneWork flushLuceneWork, Void p) {
		return flushExecutor;
	}

	@Override
	public TransactionalOperationExecutor visitDeleteByQueryWork(DeleteByQueryLuceneWork deleteByQueryLuceneWork, Void p) {
		return deleteByQueryExecutor;
	}

	private class AddSelectionExecutor implements TransactionalOperationExecutor {

		@Override
		public final void performOperation(LuceneWork work, IndexManagerSelector selector,
				WorkQueuePerIndexSplitter context) {
			IndexManager indexManager = selector.forNew(
					work.getEntityType(),
					work.getId(),
					work.getIdInString(),
					work.getDocument()
			);
			context.addToQueue( indexManager, work );
		}

	}

	private class DeleteSelectionExecutor implements TransactionalOperationExecutor {

		@Override
		public final void performOperation(LuceneWork work, IndexManagerSelector selector,
				WorkQueuePerIndexSplitter context) {
			Set<IndexManager> indexManagers = selector.forExisting(
					work.getEntityType(),
					work.getId(),
					work.getIdInString()
			);
			for ( IndexManager indexManager : indexManagers ) {
				context.addToQueue( indexManager, work );
			}
		}

	}

	private class DeleteByQuerySelectionExecutor implements TransactionalOperationExecutor {

		@Override
		public final void performOperation(LuceneWork work, IndexManagerSelector selector,
				WorkQueuePerIndexSplitter context) {
			Set<IndexManager> indexManagers = selector.forExisting(
					work.getEntityType(),
					work.getId(),
					work.getIdInString()
			);
			for ( IndexManager indexManager : indexManagers ) {
				context.addToQueue( indexManager, work );
			}
		}

	}

	/**
	 * In this exceptional case we still delegate to a streaming operation instead:
	 * no need for transactions as optimizing doesn't affect index-encoded state.
	 */
	private static class OptimizeSelectionExecutor implements TransactionalOperationExecutor {

		@Override
		public final void performOperation(LuceneWork work, IndexManagerSelector selector,
				WorkQueuePerIndexSplitter context) {
			Set<IndexManager> indexManagers = selector.all();
			for ( IndexManager indexManager : indexManagers ) {
				context.performStreamOperation( indexManager, work );
			}
		}

	}

	private static class FlushSelectionExecutor implements TransactionalOperationExecutor {

		@Override
		public final void performOperation(LuceneWork work, IndexManagerSelector selector,
				WorkQueuePerIndexSplitter context) {
			Set<IndexManager> indexManagers = selector.all();
			for ( IndexManager indexManager : indexManagers ) {
				context.performStreamOperation( indexManager, work );
			}
		}

	}

	private class PurgeAllSelectionExecutor implements TransactionalOperationExecutor {

		@Override
		public final void performOperation(LuceneWork work, IndexManagerSelector selector,
				WorkQueuePerIndexSplitter context) {
			Set<IndexManager> indexManagers = selector.forExisting(
					work.getEntityType(),
					work.getId(),
					work.getIdInString()
			);
			for ( IndexManager indexManager : indexManagers ) {
				context.addToQueue( indexManager, work );
			}
		}

	}
}
