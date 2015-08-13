/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl;

import java.util.concurrent.ConcurrentMap;

import org.hibernate.search.backend.spi.Work;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Execute final work inside a transaction.
 *
 * @author Emmanuel Bernard
 */
public class InTransactionWorkQueueSynchronization implements WorkQueueSynchronization {

	private static final Log log = LoggerFactory.make();

	/**
	 * FullTextIndexEventListener is using a WeakIdentityHashMap<Session,Synchronization>
	 * So make sure all Synchronization implementations don't have any
	 * (direct or indirect) reference to the Session.
	 */

	private final QueueingProcessor queueingProcessor;
	private boolean consumed;
	private boolean prepared;
	private final ConcurrentMap<Object, WorkQueueSynchronization> queuePerTransaction;
	private final WorkQueue queue;
	private final Object transactionIdentifier;

	/**
	 * In transaction work
	 *
	 * @param transactionIdentifier the identifier of the transaction
	 * @param queueingProcessor the {@link QueueingProcessor}
	 * @param queuePerTransaction the map containing the queue for eeach transaction
	 * @param extendedIntegrator the {@link ExtendedSearchIntegrator}
	 */
	public InTransactionWorkQueueSynchronization(Object transactionIdentifier, QueueingProcessor queueingProcessor,
												ConcurrentMap<Object, WorkQueueSynchronization> queuePerTransaction,
												ExtendedSearchIntegrator extendedIntegrator) {
		this.transactionIdentifier = transactionIdentifier;
		this.queueingProcessor = queueingProcessor;
		this.queuePerTransaction = queuePerTransaction;
		queue = new WorkQueue( extendedIntegrator );
	}

	public void add(Work work) {
		queueingProcessor.add( work, queue );
	}

	public boolean isConsumed() {
		return consumed;
	}

	@Override
	public void beforeCompletion() {
		// we are doing all the work in the before completion phase so that it is part of the transaction
		try {
			if ( prepared ) {
				log.tracef(
						"Transaction's beforeCompletion() phase already been processed, ignoring: %s", this
				);
			}
			else {
				log.tracef( "Processing Transaction's beforeCompletion() phase: %s", this );
				queueingProcessor.prepareWorks( queue );
				prepared = true;
			}

			log.tracef(
					"Processing Transaction's afterCompletion() phase for %s. Performing work.", this
			);
			queueingProcessor.performWorks( queue );
		}
		finally {
			consumed = true;
			//clean the Synchronization per Transaction
			//not needed stricto sensus but a cleaner approach and faster than the GC
			if ( queuePerTransaction != null ) {
				queuePerTransaction.remove( transactionIdentifier );
			}
		}
	}

	@Override
	public void afterCompletion(int status) {
		// nothing to do, everything was done in beforeCompletion
	}

	public void flushWorks() {
		WorkQueue subQueue = queue.splitQueue();
		queueingProcessor.prepareWorks( subQueue );
		queueingProcessor.performWorks( subQueue );
	}
}
