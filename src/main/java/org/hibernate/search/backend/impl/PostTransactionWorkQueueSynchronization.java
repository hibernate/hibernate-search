/* $Id$
 * 
 * Hibernate, Relational Persistence for Idiomatic Java
 * 
 * Copyright (c) 2009, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.backend.QueueingProcessor;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkQueue;
import org.hibernate.search.util.WeakIdentityHashMap;

/**
 * Execute some work inside a transaction synchronization
 *
 * @author Emmanuel Bernard
 */
public class PostTransactionWorkQueueSynchronization implements Synchronization {
	
	/**
	 * FullTextIndexEventListener is using a WeakIdentityHashMap<Session,Synchronization>
	 * So make sure all Synchronization implementations don't have any
	 * (direct or indirect) reference to the Session.
	 */
	
	private final QueueingProcessor queueingProcessor;
	private boolean consumed;
	private final WeakIdentityHashMap queuePerTransaction;
	private WorkQueue queue = new WorkQueue();

	/**
	 * in transaction work
	 */
	public PostTransactionWorkQueueSynchronization(QueueingProcessor queueingProcessor, WeakIdentityHashMap queuePerTransaction) {
		this.queueingProcessor = queueingProcessor;
		this.queuePerTransaction = queuePerTransaction;
	}

	public void add(Work work) {
		queueingProcessor.add( work, queue );
	}

	public boolean isConsumed() {
		return consumed;
	}

	public void beforeCompletion() {
		queueingProcessor.prepareWorks(queue);
	}

	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				queueingProcessor.performWorks(queue);
			}
			else {
				queueingProcessor.cancelWorks(queue);
			}
		}
		finally {
			consumed = true;
			//clean the Synchronization per Transaction
			//not needed stricto sensus but a cleaner approach and faster than the GC
			if (queuePerTransaction != null) queuePerTransaction.removeValue( this ); 
		}
	}

	public void flushWorks() {
		WorkQueue subQueue = queue.splitQueue();
		queueingProcessor.prepareWorks( subQueue );
		queueingProcessor.performWorks( subQueue );
	}
}
