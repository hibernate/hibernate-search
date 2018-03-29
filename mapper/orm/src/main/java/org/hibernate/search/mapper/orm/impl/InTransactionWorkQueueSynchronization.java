/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.transaction.Synchronization;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.mapping.ChangesetPojoWorker;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Execute final work inside a transaction.
 *
 * @author Emmanuel Bernard
 */
class InTransactionWorkQueueSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final ChangesetPojoWorker worker;
	private final Map<?, ?> workerPerTransaction;
	private final Object transactionIdentifier;

	InTransactionWorkQueueSynchronization(ChangesetPojoWorker worker,
			Map<?, ?> workerPerTransaction, Object transactionIdentifier) {
		this.worker = worker;
		this.workerPerTransaction = workerPerTransaction;
		this.transactionIdentifier = transactionIdentifier;
	}

	@Override
	public void beforeCompletion() {
		// we are doing all the work in the before completion phase so that it is part of the transaction
		try {
			log.tracef(
					"Processing Transaction's beforeCompletion() phase for %s. Performing work.", this
			);
			CompletableFuture<?> future = worker.execute();
			/*
			 * TODO decide whether we want the sync/async setting to be scoped per index,
			 * or per EntityManager/SearchManager, or both (with one scope overriding the other).
			 * See also PostTransactionWorkQueueSynchronization#afterCompletion, PojoSearchManagerImpl#close
			 */
			future.join();
		}
		finally {
			//clean the Synchronization per Transaction
			//not strictly required but a cleaner approach and faster than the GC
			workerPerTransaction.remove( transactionIdentifier );
		}
	}

	@Override
	public void afterCompletion(int status) {
		// nothing to do, everything was done in beforeCompletion
	}

}
