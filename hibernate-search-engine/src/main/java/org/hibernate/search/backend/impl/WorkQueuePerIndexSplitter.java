/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.hibernate.search.backend.impl;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Used by {@link org.hibernate.search.backend.impl.ContextAwareSelectionDelegate} to split a list of operations
 * according to the multiple IndexManagers it needs to be routed to.
 *
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class WorkQueuePerIndexSplitter {

	private final HashMap<String,WorkPlan> queues = new HashMap<String,WorkPlan>();

	/**
	 * @param indexManager
	 */
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
