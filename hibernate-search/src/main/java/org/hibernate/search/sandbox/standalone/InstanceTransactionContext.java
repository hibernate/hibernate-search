/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.search.sandbox.standalone;

import org.hibernate.search.backend.TransactionContext;

import javax.transaction.Synchronization;
import javax.transaction.Status;
import java.util.List;
import java.util.ArrayList;

/**
 * Transaction context that contains transaction boundaries methods.
 * While not "transactional" it allows to call Synchronization elements
 * 
 * @author Emmanuel Bernard
 */
public class InstanceTransactionContext implements TransactionContext {
	private State transactionState = State.NO_TRANSACTION;
	private final List<Synchronization> synchronizations = new ArrayList<Synchronization>(5);

	public void beginTransaction() {
		if (transactionState != State.NO_TRANSACTION) {
			throw new IllegalStateException( "Transaction context already started: " + transactionState);
		}
		transactionState = State.IN_TRANSACTION;
	}

	public void commit() {
		if ( transactionState != State.IN_TRANSACTION ) {
			throw new IllegalStateException( "Transaction context not in active state: " + transactionState);
		}
		try {
			for (Synchronization sync : synchronizations) {
				sync.beforeCompletion();
			}
			for (Synchronization sync : synchronizations) {
				sync.afterCompletion( Status.STATUS_COMMITTED );
			}
		}
		finally {
			synchronizations.clear();
			transactionState = State.TRANSACTION_CLOSED;
		}
	}

	public void rollback() {
		if ( transactionState != State.IN_TRANSACTION ) {
			throw new IllegalStateException( "Transaction context not in active state: " + transactionState);
		}
		try {
			for (Synchronization sync : synchronizations) {
				sync.beforeCompletion();
			}
			for (Synchronization sync : synchronizations) {
				sync.afterCompletion( Status.STATUS_ROLLEDBACK );
			}
		}
		finally {
			synchronizations.clear();
			transactionState = State.TRANSACTION_CLOSED;
		}
	}

	public boolean isTransactionInProgress() {
		return transactionState == State.IN_TRANSACTION;
	}

	public Object getTransactionIdentifier() {
		return this;
	}

	public void registerSynchronization(Synchronization synchronization) {
		synchronizations.add( synchronization );
	}

	private static enum State {
		NO_TRANSACTION,
		IN_TRANSACTION,
		TRANSACTION_CLOSED
	}
}
