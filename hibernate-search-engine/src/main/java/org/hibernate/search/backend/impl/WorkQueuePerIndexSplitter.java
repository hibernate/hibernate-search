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

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.indexes.spi.IndexManager;

/**
 * Used by {@link ContextAwareSelectionDelegate} to split a list of operations
 * according to the multiple IndexManagers it needs to be routed to.
 * 
 * @author Sanne Grinovero <sanne@hibernate.org> (C) 2011 Red Hat Inc.
 */
public class WorkQueuePerIndexSplitter {
	
	private IdentityHashMap<IndexManager,List<LuceneWork>> queues = new IdentityHashMap<IndexManager,List<LuceneWork>>();

	/**
	 * @param indexManager
	 */
	public List<LuceneWork> getIndexManagerQueue(IndexManager indexManager) {
		List<LuceneWork> list = queues.get( indexManager );
		if ( list == null ) {
			list = new LinkedList<LuceneWork>();
			queues.put( indexManager, list );
		}
		return list;
	}

	/**
	 * Send all operations stored so far to the backend to be performed, atomically and/or transactionally
	 * if supported/enabled by each specific backend.
	 */
	public void commitOperations(IndexingMonitor monitor) {
		// FIXME move executor here to parallel work - optionally? See HSEARCH-826
		for ( Entry<IndexManager,List<LuceneWork>> entry : queues.entrySet() ) {
			entry.getKey().performOperations( entry.getValue(), monitor );
		}
	}

}
