/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.mapper.orm.logging.impl.IndexingLog;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;

/**
 * Executes the indexing plan outside the transaction, after the commit.
 */
class AfterCommitIndexingPlanSynchronization implements Synchronization {

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
		IndexingLog.INSTANCE.tracef( "Processing Transaction's beforeCompletion() phase for %s.", transactionIdentifier );
		indexingPlan.process();
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED == i ) {
				IndexingLog.INSTANCE.tracef( "Processing Transaction's afterCompletion() phase for %s. Executing indexing plan.",
						transactionIdentifier );
				synchronizationStrategy.executeAndSynchronize( indexingPlan );
			}
			else {
				IndexingLog.INSTANCE.tracef(
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
