// $Id$
package org.hibernate.search.backend.impl;

import java.io.Serializable;

import javax.transaction.Synchronization;

import org.hibernate.AssertionFailure;
import org.hibernate.Transaction;
import org.hibernate.event.EventSource;
import org.hibernate.event.FlushEventListener;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.event.IndexWorkFlushEventListener;
import org.hibernate.search.util.LoggerFactory;
import org.slf4j.Logger;

/**
 * Implementation of the transactional context on top of an EventSource (Session)
 * 
 * @author Navin Surtani  - navin@surtani.org
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class EventSourceTransactionContext implements TransactionContext, Serializable {
	
	private static final Logger log = LoggerFactory.make();
	
	private final EventSource eventSource;
	private final IndexWorkFlushEventListener flushListener;

	//constructor time is too early to define the value of realTxInProgress,
	//postpone it, otherwise doing
	// " openSession - beginTransaction "
	//will behave as "out of transaction" in the whole session lifespan.
	private Boolean realTxInProgress = null;
	
	public EventSourceTransactionContext(EventSource eventSource) {
		this.eventSource = eventSource;
		this.flushListener = findIndexWorkFlushEventListener();
	}

	public Object getTransactionIdentifier() {
		if ( isRealTransactionInProgress() ) {
			return eventSource.getTransaction();
		}
		else {
			return eventSource;
		}
	}

	public void registerSynchronization(Synchronization synchronization) {
		if ( isRealTransactionInProgress() ) {
			Transaction transaction = eventSource.getTransaction();
			transaction.registerSynchronization( synchronization );
		}
		else {
			if ( flushListener != null ) {
				flushListener.addSynchronization( eventSource, synchronization );
			}
			else {
				//It appears we are flushing out of transaction and have no way to perform the index update
				//Not expected: see check in isTransactionInProgress()
				throw new AssertionFailure( "On flush out of transaction: IndexWorkFlushEventListener not registered" );
			}
		}
	}
	
	private IndexWorkFlushEventListener findIndexWorkFlushEventListener() {
		FlushEventListener[] flushEventListeners = eventSource.getListeners().getFlushEventListeners();
		for (FlushEventListener listener : flushEventListeners) {
			if ( listener.getClass().equals( IndexWorkFlushEventListener.class ) ) {
				return (IndexWorkFlushEventListener) listener;
			}
		}
		log.debug( "No IndexWorkFlushEventListener was registered" );
		return null;
	}

	//The code is not really fitting the method name;
	//(unless you consider a flush as a mini-transaction)
	//This is because we want to behave as "inTransaction" if the flushListener is registered.
	public boolean isTransactionInProgress() {
		// either it is a real transaction, or if we are capable to manage this in the IndexWorkFlushEventListener
		return isRealTransactionInProgress() || flushListener != null;
	}
	
	private boolean isRealTransactionInProgress() {
		if ( realTxInProgress == null ) {
			realTxInProgress = eventSource.isTransactionInProgress();
		}
		return realTxInProgress;
	}
	
}
