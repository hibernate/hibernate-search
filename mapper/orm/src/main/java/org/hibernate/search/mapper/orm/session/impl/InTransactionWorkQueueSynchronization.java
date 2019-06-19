/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import javax.transaction.Synchronization;

import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.session.AutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoWorkPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Execute final work inside a transaction.
 *
 * @author Emmanuel Bernard
 */
public class InTransactionWorkQueueSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoWorkPlan workPlan;
	private final Map<?, ?> workPlanPerTransaction;
	private final Object transactionIdentifier;
	private final AutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	public InTransactionWorkQueueSynchronization(PojoWorkPlan workPlan,
			Map<?, ?> workPlanPerTransaction, Object transactionIdentifier,
			AutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		this.workPlan = workPlan;
		this.workPlanPerTransaction = workPlanPerTransaction;
		this.transactionIdentifier = transactionIdentifier;
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public void beforeCompletion() {
		// we are doing all the work in the before completion phase so that it is part of the transaction
		try {
			log.tracef(
					"Processing Transaction's beforeCompletion() phase for %s. Performing work.", this
			);
			CompletableFuture<?> future = workPlan.execute();
			synchronizationStrategy.handleFuture( future );
		}
		finally {
			//clean the Synchronization per Transaction
			workPlanPerTransaction.remove( transactionIdentifier );
		}
	}

	@Override
	public void afterCompletion(int status) {
		// nothing to do, everything was done in beforeCompletion
	}

}
