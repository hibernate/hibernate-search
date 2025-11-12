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
 * Executes the indexing plan inside the transaction, before the commit.
 */
class BeforeCommitIndexingPlanSynchronization implements Synchronization {

	private final PojoIndexingPlan indexingPlan;
	private final HibernateOrmSearchSessionExtension sessionExtension;
	private final Transaction transactionIdentifier;
	private final ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy;

	BeforeCommitIndexingPlanSynchronization(PojoIndexingPlan indexingPlan,
			HibernateOrmSearchSessionExtension sessionExtension, Transaction transactionIdentifier,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlan = indexingPlan;
		this.sessionExtension = sessionExtension;
		this.transactionIdentifier = transactionIdentifier;
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public void beforeCompletion() {
		IndexingLog.INSTANCE.afterCompletionExecuting( transactionIdentifier );
		synchronizationStrategy.executeAndSynchronize( indexingPlan );
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED != i ) {
				IndexingLog.INSTANCE.afterCompletionCanceling( transactionIdentifier, i );
				indexingPlan.discard();
			}
		}
		finally {
			//clean the Synchronization per Transaction
			sessionExtension.clear( transactionIdentifier );
		}
	}
}
