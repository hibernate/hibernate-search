/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.AddLuceneWork;
import org.hibernate.search.backend.DeleteLuceneWork;
import org.hibernate.search.backend.FlushLuceneWork;
import org.hibernate.search.backend.IndexWorkVisitor;
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
import org.hibernate.search.backend.spi.DeleteByQueryLuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.store.IndexShardingStrategy;

/**
 * This visitor applies the selection logic from the plugged IndexShardingStrategies to stream operations, as used by
 * optimize() and batching operations. Using a visitor/selector pattern for different implementations of
 * addAsPayLoadsToQueue depending on the type of LuceneWork.
 * <p>
 * Implementation note: This {@link IndexWorkVisitor} implementation intentionally does not perform the actual logic
 * within the individual visit methods themselves but rather returns a delegate class for that purpose. This is to avoid
 * the need for the allocation of a parameter object with the required input data, instead a method with the required
 * parameters is exposed on said delegate.
 *
 * @author Sanne Grinovero, Martin Braun
 */
public class StreamingOperationExecutorSelector implements IndexWorkVisitor<Void, StreamingOperationExecutor> {

	private final AddSelectionExecutor addExecutor = new AddSelectionExecutor();
	private final DeleteSelectionExecutor deleteExecutor = new DeleteSelectionExecutor();
	private final AllSelectionExecutor allManagersExecutor = new AllSelectionExecutor();
	private final PurgeAllSelectionExecutor purgeExecutor = new PurgeAllSelectionExecutor();
	private final DeleteByQuerySelectionExecutor deleteByQueryExecutor = new DeleteByQuerySelectionExecutor();

	public static final StreamingOperationExecutorSelector INSTANCE = new StreamingOperationExecutorSelector();

	private StreamingOperationExecutorSelector() {
		// use INSTANCE as this delegator is stateless
	}

	@Override
	public StreamingOperationExecutor visitAddWork(AddLuceneWork addLuceneWork, Void p) {
		return addExecutor;
	}

	@Override
	public StreamingOperationExecutor visitUpdateWork(UpdateLuceneWork updateLuceneWork, Void p) {
		return addExecutor;
	}

	@Override
	public StreamingOperationExecutor visitDeleteWork(DeleteLuceneWork deleteLuceneWork, Void p) {
		return deleteExecutor;
	}

	@Override
	public StreamingOperationExecutor visitOptimizeWork(OptimizeLuceneWork optimizeLuceneWork, Void p) {
		return allManagersExecutor;
	}

	@Override
	public StreamingOperationExecutor visitPurgeAllWork(PurgeAllLuceneWork purgeAllLuceneWork, Void p) {
		return purgeExecutor;
	}

	@Override
	public StreamingOperationExecutor visitFlushWork(FlushLuceneWork flushLuceneWork, Void p) {
		return allManagersExecutor;
	}

	@Override
	public StreamingOperationExecutor visitDeleteByQueryWork(DeleteByQueryLuceneWork deleteByQueryLuceneWork, Void p) {
		return deleteByQueryExecutor;
	}

	private static class DeleteByQuerySelectionExecutor implements StreamingOperationExecutor {

		@Override
		public void performStreamOperation(LuceneWork work, IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion( work.getEntityClass(), work.getId(), work.getIdInString() );
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

	private static class AddSelectionExecutor implements StreamingOperationExecutor {

		@Override
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

	private static class DeleteSelectionExecutor implements StreamingOperationExecutor {

		@Override
		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

	private static class AllSelectionExecutor implements StreamingOperationExecutor {

		@Override
		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

	private static class PurgeAllSelectionExecutor implements StreamingOperationExecutor {

		@Override
		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion(
					work.getEntityClass(),
					work.getId(),
					work.getIdInString()
			);
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}
	}
}
