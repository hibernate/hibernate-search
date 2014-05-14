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

	@Override
	public StreamingOperationSelectionDelegate getDelegate(AddLuceneWork addLuceneWork) {
		return addDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate getDelegate(UpdateLuceneWork addLuceneWork) {
		return addDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate getDelegate(DeleteLuceneWork deleteLuceneWork) {
		return deleteDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate getDelegate(OptimizeLuceneWork optimizeLuceneWork) {
		return allManagersDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate getDelegate(PurgeAllLuceneWork purgeAllLuceneWork) {
		return purgeDelegate;
	}

	@Override
	public StreamingOperationSelectionDelegate getDelegate(FlushLuceneWork flushLuceneWork) {
		return allManagersDelegate;
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
