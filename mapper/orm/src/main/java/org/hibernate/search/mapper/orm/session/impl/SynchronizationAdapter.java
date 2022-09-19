/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session.impl;

import java.lang.invoke.MethodHandles;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;

import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

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

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

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
		log.tracef(
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
			throw log.synchronizationBeforeTransactionFailure( e.getMessage(), e );
		}
	}

	@Override
	public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor sessionImplementor) {
		try {
			doAfterCompletion( success ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK );
		}
		catch (RuntimeException e) {
			throw log.synchronizationAfterTransactionFailure( e.getMessage(), e );
		}
	}

	private void doBeforeCompletion() {
		if ( beforeExecuted ) {
			log.tracef(
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
			log.tracef(
					"Transaction's afterCompletion() phase already been processed, ignoring: %s", delegate
			);
		}
		else {
			delegate.afterCompletion( status );
			afterExecuted = true;
		}
	}
}
