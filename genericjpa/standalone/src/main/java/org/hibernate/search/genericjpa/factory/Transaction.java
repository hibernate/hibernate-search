/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.genericjpa.factory;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.search.genericjpa.transaction.TransactionContext;

public class Transaction implements TransactionContext {

	private boolean progress = true;
	private List<Synchronization> syncs = new ArrayList<>();

	@Override
	public boolean isTransactionInProgress() {
		return this.progress;
	}

	@Override
	public Object getTransactionIdentifier() {
		return this;
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		this.syncs.add( synchronization );
	}

	/**
	 * @throws IllegalStateException if already commited/rolledback
	 */
	public void commit() {
		if ( !this.progress ) {
			throw new IllegalStateException( "can't commit - No Search Transaction is in Progress!" );
		}
		this.progress = false;
		this.syncs.forEach( Synchronization::beforeCompletion );

		for ( Synchronization sync : this.syncs ) {
			sync.afterCompletion( Status.STATUS_COMMITTED );
		}
	}

	/**
	 * @throws IllegalStateException if already commited/rolledback
	 */
	public void rollback() {
		if ( !this.progress ) {
			throw new IllegalStateException( "can't rollback - No Search Transaction is in Progress!" );
		}
		this.progress = false;
		this.syncs.forEach( Synchronization::beforeCompletion );

		for ( Synchronization sync : this.syncs ) {
			sync.afterCompletion( Status.STATUS_ROLLEDBACK );
		}
	}

}
