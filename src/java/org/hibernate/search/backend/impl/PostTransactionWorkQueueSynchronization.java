//$Id$
package org.hibernate.search.backend.impl;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.backend.QueueingProcessor;
import org.hibernate.search.backend.Work;
import org.hibernate.search.backend.WorkQueue;
import org.hibernate.search.util.WeakIdentityHashMap;

/**
 * Execute some work inside a transaction sychronization
 *
 * @author Emmanuel Bernard
 */
public class PostTransactionWorkQueueSynchronization implements Synchronization {
	private QueueingProcessor queueingProcessor;
	private boolean consumed;
	private WeakIdentityHashMap queuePerTransaction;
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
}
