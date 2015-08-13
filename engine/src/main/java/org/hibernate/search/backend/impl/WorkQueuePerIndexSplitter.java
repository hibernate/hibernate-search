/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Used by {@link org.hibernate.search.backend.impl.TransactionalOperationExecutor} to split a list of operations
 * according to the multiple IndexManagers it needs to be routed to.
 *
 * @author Sanne Grinovero (C) 2011 Red Hat Inc.
 */
public class WorkQueuePerIndexSplitter {

	private final HashMap<String,WorkPlan> queues = new HashMap<String,WorkPlan>();

	public List<LuceneWork> getIndexManagerQueue(final IndexManager indexManager) {
		final String indexName = indexManager.getIndexName();
		WorkPlan plan = queues.get( indexName );
		if ( plan == null ) {
			plan = new WorkPlan( indexManager );
			queues.put( indexName, plan );
		}
		return plan.queue;
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
			plan.indexManager.performOperations( plan.queue, monitor );
		}
	}

	private static class WorkPlan {
		private final IndexManager indexManager;
		private final LinkedList<LuceneWork> queue = new LinkedList<LuceneWork>();
		WorkPlan(IndexManager indexManager) {
			this.indexManager = indexManager;
		}
	}

}
