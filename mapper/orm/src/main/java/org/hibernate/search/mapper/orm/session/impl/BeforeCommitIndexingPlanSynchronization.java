/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.pojo.work.spi.ConfiguredIndexingPlanSynchronizationStrategy;
import org.hibernate.search.mapper.pojo.work.spi.PojoIndexingPlan;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * Executes the indexing plan inside the transaction, before the commit.
 */
class BeforeCommitIndexingPlanSynchronization implements Synchronization {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final PojoIndexingPlan indexingPlan;
	private final HibernateOrmSearchSessionHolder sessionHolder;
	private final Transaction transactionIdentifier;
	private final ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy;

	BeforeCommitIndexingPlanSynchronization(PojoIndexingPlan indexingPlan,
			HibernateOrmSearchSessionHolder sessionHolder, Transaction transactionIdentifier,
			ConfiguredIndexingPlanSynchronizationStrategy synchronizationStrategy) {
		this.indexingPlan = indexingPlan;
		this.sessionHolder = sessionHolder;
		this.transactionIdentifier = transactionIdentifier;
		this.synchronizationStrategy = synchronizationStrategy;
	}

	@Override
	public void beforeCompletion() {
		log.tracef( "Processing Transaction's afterCompletion() phase for %s. Executing indexing plan.",
				transactionIdentifier );
		synchronizationStrategy.executeAndSynchronize( indexingPlan );
	}

	@Override
	public void afterCompletion(int i) {
		try {
			if ( Status.STATUS_COMMITTED != i ) {
				log.tracef(
						"Processing Transaction's afterCompletion() phase for %s. Cancelling work due to transaction status %d",
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
