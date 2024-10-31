/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.mapper.orm.session.impl;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.orm.logging.impl.OrmSpecificLog;

/**
 * An adapter for synchronizations, allowing to register them as
 * {@link BeforeTransactionCompletionProcess} or {@link AfterTransactionCompletionProcess} too,
 * without running the risk of executing their methods twice.
 * <p>
 * Also, suppresses any call to {@link Synchronization#afterCompletion(int)} so that
 * it can be executed later, in {@link AfterTransactionCompletionProcess#doAfterTransactionCompletion(boolean, SharedSessionContractImplementor)}.
 */
class SynchronizationAdapter
		implements Synchronization,
		BeforeTransactionCompletionProcess, AfterTransactionCompletionProcess {


	private final Synchronization delegate;
	private boolean beforeExecuted = false;
	private boolean afterExecuted = false;

	SynchronizationAdapter(Synchronization delegate) {
		this.delegate = delegate;
	}

	@Override
	public void beforeCompletion() {
		doBeforeCompletion();
	}

	@Override
	public void afterCompletion(int status) {
		OrmSpecificLog.INSTANCE.tracef(
				"Transaction's afterCompletion is expected to be executed"
						+ " through the AfterTransactionCompletionProcess interface, ignoring: %s",
				delegate
		);
	}

	@Override
	public void doBeforeTransactionCompletion(SessionImplementor sessionImplementor) {
		try {
			doBeforeCompletion();
		}
		catch (RuntimeException e) {
			throw OrmSpecificLog.INSTANCE.synchronizationBeforeTransactionFailure( e.getMessage(), e );
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor sessionImplementor) {
		try {
			doAfterCompletion( success ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK );
		}
		catch (RuntimeException e) {
			throw OrmSpecificLog.INSTANCE.synchronizationAfterTransactionFailure( e.getMessage(), e );
		}
	}

	private void doBeforeCompletion() {
		if ( beforeExecuted ) {
			OrmSpecificLog.INSTANCE.tracef(
					"Transaction's beforeCompletion() phase already been processed, ignoring: %s", delegate
			);
		}
		else {
			delegate.beforeCompletion();
			beforeExecuted = true;
		}
	}

	private void doAfterCompletion(int status) {
		if ( afterExecuted ) {
			OrmSpecificLog.INSTANCE.tracef(
					"Transaction's afterCompletion() phase already been processed, ignoring: %s", delegate
			);
		}
		else {
			delegate.afterCompletion( status );
			afterExecuted = true;
		}
	}
}
