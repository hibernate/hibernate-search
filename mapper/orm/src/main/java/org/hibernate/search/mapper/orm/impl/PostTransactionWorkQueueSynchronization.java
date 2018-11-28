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
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.util.impl.common.LoggerFactory;

/**
 * Execute final work in the after transaction synchronization.
 *
 * @author Emmanuel Bernard
 */
public class PostTransactionWorkQueueSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkPlan workPlan;
	private final Map<?, ?> workPlanPerTransaction;
	private final Object transactionIdentifier;

	PostTransactionWorkQueueSynchronization(PojoWorkPlan workPlan,
			Map<?, ?> workPlanPerTransaction, Object transactionIdentifier) {
		this.workPlan = workPlan;
		this.workPlanPerTransaction = workPlanPerTransaction;
		this.transactionIdentifier = transactionIdentifier;
	}

	@Override
	public void beforeCompletion() {
		log.tracef( "Processing Transaction's beforeCompletion() phase: %s", this );
		workPlan.prepare();
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				log.tracef( "Processing Transaction's afterCompletion() phase for %s. Performing work.", this );
				CompletableFuture<?> future = workPlan.execute();
				/*
				 * TODO decide whether we want the sync/async setting to be scoped per index,
				 * or per EntityManager/SearchManager, or both (with one scope overriding the other).
				 * See also InTransactionWorkQueueSynchronization#beforeCompletion
				 */
				future.join();
			}
			else {
				log.tracef(
						"Processing Transaction's afterCompletion() phase for %s. Cancelling work due to transaction status %d",
						this,
						i
				);
				// FIXME send some signal to the workPlan to release resources if necessary?
			}
		}
		finally {
			//clean the Synchronization per Transaction
			workPlanPerTransaction.remove( transactionIdentifier );
		}
	}
}
