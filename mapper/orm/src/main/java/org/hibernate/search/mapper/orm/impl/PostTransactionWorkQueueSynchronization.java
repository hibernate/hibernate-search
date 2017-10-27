/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.impl;

import java.util.Map;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.util.spi.LoggerFactory;

/**
 * Execute final work in the after transaction synchronization.
 *
 * @author Emmanuel Bernard
 */
public class PostTransactionWorkQueueSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class );

	private final ChangesetPojoWorker worker;
	private final Map<?, ?> workerPerTransaction;
	private final Object transactionIdentifier;

	PostTransactionWorkQueueSynchronization(ChangesetPojoWorker worker,
			Map<?, ?> workerPerTransaction, Object transactionIdentifier) {
		this.worker = worker;
		this.workerPerTransaction = workerPerTransaction;
		this.transactionIdentifier = transactionIdentifier;
	}

	@Override
	public void beforeCompletion() {
		log.tracef( "Processing Transaction's beforeCompletion() phase: %s", this );
		worker.prepare();
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				log.tracef( "Processing Transaction's afterCompletion() phase for %s. Performing work.", this );
				worker.execute();
			}
			else {
				log.tracef(
						"Processing Transaction's afterCompletion() phase for %s. Cancelling work due to transaction status %d",
						this,
						i
				);
				// FIXME send some signal to the worker to release resources if necessary?
			}
		}
		finally {
			//clean the Synchronization per Transaction
			//not strictly required but a cleaner approach and faster than the GC
			workerPerTransaction.remove( transactionIdentifier );
		}
	}
}
