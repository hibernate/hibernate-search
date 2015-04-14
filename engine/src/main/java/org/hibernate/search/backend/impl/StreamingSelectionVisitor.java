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
import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.OptimizeLuceneWork;
import org.hibernate.search.backend.PurgeAllLuceneWork;
import org.hibernate.search.backend.UpdateLuceneWork;
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
public class StreamingSelectionVisitor implements IndexWorkVisitor<Void, StreamingOperationSelectionDelegate> {

	private final AddSelectionDelegate addDelegate = new AddSelectionDelegate();
	private final DeleteSelectionDelegate deleteDelegate = new DeleteSelectionDelegate();
	private final AllSelectionDelegate allManagersDelegate = new AllSelectionDelegate();
	private final PurgeAllSelectionDelegate purgeDelegate = new PurgeAllSelectionDelegate();
	private final DeleteByQuerySelectionDelegate deleteByQueryDelegate = new DeleteByQuerySelectionDelegate();

	public static final StreamingSelectionVisitor INSTANCE = new StreamingSelectionVisitor();

	private StreamingSelectionVisitor() {
		// use INSTANCE as this delegator is stateless
	}

	@Override
	public StreamingOperationSelectionDelegate visitAddWork(AddLuceneWork addLuceneWork, Void p) {
		return addDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate visitUpdateWork(UpdateLuceneWork updateLuceneWork, Void p) {
		return addDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate visitDeleteWork(DeleteLuceneWork deleteLuceneWork, Void p) {
		return deleteDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate visitOptimizeWork(OptimizeLuceneWork optimizeLuceneWork, Void p) {
		return allManagersDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate visitPurgeAllWork(PurgeAllLuceneWork purgeAllLuceneWork, Void p) {
		return purgeDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate visitFlushWork(FlushLuceneWork flushLuceneWork, Void p) {
		return allManagersDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate visitDeleteByQueryWork(DeleteByQueryLuceneWork deleteByQueryLuceneWork, Void p) {
		return deleteByQueryDelegate;
	}

	private static class DeleteByQuerySelectionDelegate implements StreamingOperationSelectionDelegate {

		@Override
		public void performStreamOperation(LuceneWork work, IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForDeletion( work.getEntityClass(), work.getId(), work.getIdInString() );
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

	private static class AddSelectionDelegate implements StreamingOperationSelectionDelegate {

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

	private static class DeleteSelectionDelegate implements StreamingOperationSelectionDelegate {

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

	private static class AllSelectionDelegate implements StreamingOperationSelectionDelegate {

		@Override
		public final void performStreamOperation(LuceneWork work,
				IndexShardingStrategy shardingStrategy, IndexingMonitor monitor, boolean forceAsync) {
			IndexManager[] indexManagers = shardingStrategy.getIndexManagersForAllShards();
			for ( IndexManager indexManager : indexManagers ) {
				indexManager.performStreamOperation( work, monitor, forceAsync );
			}
		}

	}

	private static class PurgeAllSelectionDelegate implements StreamingOperationSelectionDelegate {

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
