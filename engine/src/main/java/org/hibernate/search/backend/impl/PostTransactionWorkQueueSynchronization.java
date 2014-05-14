/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
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
