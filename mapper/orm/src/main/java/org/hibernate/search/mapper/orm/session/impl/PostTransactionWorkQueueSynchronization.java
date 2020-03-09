/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import java.util.Map;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.search.mapper.orm.automaticindexing.session.impl.ConfiguredAutomaticIndexingSynchronizationStrategy;
import org.hibernate.search.mapper.orm.common.EntityReference;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Execute final work in the after transaction synchronization.
 *
 * @author Emmanuel Bernard
 */
class PostTransactionWorkQueueSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexingPlan<EntityReference> indexingPlan;
	private final Map<?, ?> indexingPlanPerTransaction;
	private final Object transactionIdentifier;
	private final ConfiguredAutomaticIndexingSynchronizationStrategy synchronizationStrategy;

	PostTransactionWorkQueueSynchronization(PojoIndexingPlan<EntityReference> indexingPlan,
			Map<?, ?> indexingPlanPerTransaction, Object transactionIdentifier,
			ConfiguredAutomaticIndexingSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlan = indexingPlan;
		this.indexingPlanPerTransaction = indexingPlanPerTransaction;
		this.transactionIdentifier = transactionIdentifier;
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public void beforeCompletion() {
		log.tracef( "Processing Transaction's beforeCompletion() phase: %s", this );
		indexingPlan.process();
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				log.tracef( "Processing Transaction's afterCompletion() phase for %s. Performing work.", this );
				synchronizationStrategy.executeAndSynchronize( indexingPlan );
			}
			else {
				log.tracef(
						"Processing Transaction's afterCompletion() phase for %s. Cancelling work due to transaction status %d",
						this,
						i
				);
				indexingPlan.discard();
			}
		}
		finally {
			//clean the Synchronization per Transaction
			indexingPlanPerTransaction.remove( transactionIdentifier );
		}
	}
}
