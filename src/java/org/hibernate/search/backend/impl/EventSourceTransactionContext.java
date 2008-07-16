package org.hibernate.search.backend.impl;

import java.io.Serializable;
import javax.transaction.Synchronization;

import org.hibernate.Transaction;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.event.EventSource;

/**
 * Implementation of the transactional context on top of an EventSource (Session)
 * 
 * @author Navin Surtani  - navin@surtani.org
 */
public class EventSourceTransactionContext implements TransactionContext, Serializable {
	EventSource eventSource;

	public EventSourceTransactionContext(EventSource eventSource) {
		this.eventSource = eventSource;
	}

	public Object getTransactionIdentifier() {
		return eventSource.getTransaction();
	}

	public void registerSynchronization(Synchronization synchronization) {
		Transaction transaction = eventSource.getTransaction();
		transaction.registerSynchronization( synchronization );
	}

	public boolean isTransactionInProgress() {
		return eventSource.isTransactionInProgress();
	}

}
