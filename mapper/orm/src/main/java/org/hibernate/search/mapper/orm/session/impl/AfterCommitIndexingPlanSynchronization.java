/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;
import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Executes the indexing plan outside the transaction, after the commit.
 */
class AfterCommitIndexingPlanSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexingPlan indexingPlan;
	private final HibernateOrmSearchSessionHolder sessionHolder;
	private final Transaction transactionIdentifier;
	private final ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy;

	AfterCommitIndexingPlanSynchronization(PojoIndexingPlan indexingPlan,
			HibernateOrmSearchSessionHolder sessionHolder, Transaction transactionIdentifier,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlan = indexingPlan;
		this.sessionHolder = sessionHolder;
		this.transactionIdentifier = transactionIdentifier;
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public void beforeCompletion() {
		log.tracef( "Processing Transaction's beforeCompletion() phase for %s.", transactionIdentifier );
		indexingPlan.process();
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				log.tracef( "Processing Transaction's afterCompletion() phase for %s. Executing indexing plan.", transactionIdentifier );
				synchronizationStrategy.executeAndSynchronize( indexingPlan );
			}
			else {
				log.tracef(
						"Processing Transaction's afterCompletion() phase for %s. Cancelling indexing plan due to transaction status %d",
						transactionIdentifier,
						i
				);
				indexingPlan.discard();
			}
		}
		finally {
			//clean the Synchronization per Transaction
			sessionHolder.clear( transactionIdentifier );
		}
	}
}
