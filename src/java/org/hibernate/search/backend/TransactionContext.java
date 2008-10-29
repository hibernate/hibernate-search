// $Id$
package org.hibernate.search.backend;

import javax.transaction.Synchronization;

/**
 * Contract needed by Hibernate Search to batch changes per transaction.
 *
 * @author Navin Surtani  - navin@surtani.org
 */
public interface TransactionContext {
	/**
	 * @return A boolean indicating whether a transaction is in progress or not.
	 */
	public boolean isTransactionInProgress();

	/**
	 * @return a transaction object.
	 */
	public Object getTransactionIdentifier();

	/**
	 * Register the given synchronization.
	 * 
 	 * @param synchronization synchronization to register
	 */
	public void registerSynchronization(Synchronization synchronization);
}
