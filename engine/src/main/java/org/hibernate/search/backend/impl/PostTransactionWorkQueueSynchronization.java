/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
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

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.hibernate.search.util.impl.WeakIdentityHashMap;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Execute some work inside a transaction synchronization
 *
 * @author Emmanuel Bernard
 */
public class PostTransactionWorkQueueSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make();

	/**
	 * FullTextIndexEventListener is using a WeakIdentityHashMap<Session,Synchronization>
	 * So make sure all Synchronization implementations don't have any
	 * (direct or indirect) reference to the Session.
	 */

	private final QueueingProcessor queueingProcessor;
	private boolean consumed;
	private boolean prepared;
	private final WeakIdentityHashMap queuePerTransaction;
	private final WorkQueue queue;

	/**
	 * in transaction work
	 */
	public PostTransactionWorkQueueSynchronization(QueueingProcessor queueingProcessor, WeakIdentityHashMap queuePerTransaction,
			SearchFactoryImplementor searchFactoryImplementor) {
		this.queueingProcessor = queueingProcessor;
		this.queuePerTransaction = queuePerTransaction;
		queue = new WorkQueue( searchFactoryImplementor );
	}

	public void add(Work work) {
		queueingProcessor.add( work, queue );
	}

	public boolean isConsumed() {
		return consumed;
	}

	@Override
	public void beforeCompletion() {
		if ( prepared ) {
			if ( log.isTraceEnabled() ) {
				log.tracef(
						"Transaction's beforeCompletion() phase already been processed, ignoring: %s", this.toString()
				);
			}
		}
		else {
			if ( log.isTraceEnabled() ) {
				log.tracef( "Processing Transaction's beforeCompletion() phase: %s", this.toString() );
			}
			queueingProcessor.prepareWorks( queue );
			prepared = true;
		}
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				if ( log.isTraceEnabled() ) {
					log.tracef(
							"Processing Transaction's afterCompletion() phase for %s. Performing work.", this.toString()
					);
				}
				queueingProcessor.performWorks( queue );
			}
			else {
				if ( log.isTraceEnabled() ) {
					log.tracef(
							"Processing Transaction's afterCompletion() phase for %s. Cancelling work due to transaction status %d",
							this.toString(),
							i
					);
				}
				queueingProcessor.cancelWorks( queue );
			}
		}
		finally {
			consumed = true;
			//clean the Synchronization per Transaction
			//not needed stricto sensus but a cleaner approach and faster than the GC
			if ( queuePerTransaction != null ) {
				queuePerTransaction.removeValue( this );
			}
		}
	}

	public void flushWorks() {
		WorkQueue subQueue = queue.splitQueue();
		queueingProcessor.prepareWorks( subQueue );
		queueingProcessor.performWorks( subQueue );
	}
}
