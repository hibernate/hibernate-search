package org.hibernate.search.transaction;

import java.io.Serializable;
import javax.transaction.Synchronization;

/**
 * Contract needed by Hibernate Search to bach changes per transactio
 *
 * @author Navin Surtani  - navin@surtani.org
 */
public interface TransactionContext {
	/**
	 * @return A boolean whether a transaction is in progress or not.
	 */
	public boolean isTransactionInProgress();

	/**
	 * @return a transaction object.
	 */
	public Object getTransactionIdentifier();

	/**
	 * register the givne synchronization
	 * 
 	 * @param synchronization synchronization to register
	 */
	public void registerSynchronization(Synchronization synchronization);
}
