/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.function.Predicate;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.indexes.impl.IndexManagerHolder;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Used by {@link org.hibernate.search.backend.impl.TransactionalOperationExecutor} to split a list of operations
 * according to the multiple IndexManagers it needs to be routed to.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class WorkQueuePerIndexSplitter {

	private final IndexManagerHolder indexManagerHolder;
	private final Predicate<IndexManager> indexManagerFilter;
	private final HashMap<String,WorkPlan> queues = new HashMap<String,WorkPlan>();

	public WorkQueuePerIndexSplitter(IndexManagerHolder indexManagerHolder,
			Predicate<IndexManager> indexManagerFilter) {
		this.indexManagerHolder = indexManagerHolder;
		this.indexManagerFilter = indexManagerFilter;
	}

	public void addToQueue(IndexManager indexManager, LuceneWork work) {
		if ( !indexManagerFilter.test( indexManager ) ) {
			return;
		}
		String indexName = indexManager.getIndexName();
		WorkPlan plan = queues.get( indexName );
		if ( plan == null ) {
			plan = new WorkPlan( indexManagerHolder.getBackendQueueProcessor( indexName ) );
			queues.put( indexName, plan );
		}
		plan.queue.add( work );
	}

	public void performStreamOperation(IndexManager indexManager, LuceneWork work) {
		if ( !indexManagerFilter.test( indexManager ) ) {
			return;
		}
		indexManager.performStreamOperation( work, null, false );
	}

	/**
	 * Send all operations stored so far to the backend to be performed, atomically and/or transactionally
	 * if supported/enabled by each specific backend.
	 *
	 * @param monitor a {@link org.hibernate.search.backend.IndexingMonitor} object.
	 */
	public void commitOperations(IndexingMonitor monitor) {
		// FIXME move executor here to parallel work - optionally? See HSEARCH-826
		for ( WorkPlan plan : queues.values() ) {
			plan.backendQueueProcessor.applyWork( plan.queue, monitor );
		}
	}

	private static class WorkPlan {
		private final BackendQueueProcessor backendQueueProcessor;
		private final LinkedList<LuceneWork> queue = new LinkedList<LuceneWork>();
		WorkPlan(BackendQueueProcessor backendQueueProcessor) {
			this.backendQueueProcessor = backendQueueProcessor;
		}
	}

}
