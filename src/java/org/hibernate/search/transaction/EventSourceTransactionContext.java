package org.hibernate.search.transaction;

import org.hibernate.Transaction;
import org.hibernate.event.EventSource;

import javax.transaction.Synchronization;

/**
@author Navin Surtani  - navin@surtani.org
 */
public class EventSourceTransactionContext implements TransactionContext
{
   EventSource eventSource;

   public EventSourceTransactionContext(EventSource eventSource)
   {
      this.eventSource = eventSource;
   }

   public Object getTransactionIdentifier()
   {
      return eventSource.getTransaction();
   }

   public void registerSynchronization(Synchronization synchronization)
   {
       Transaction transaction = eventSource.getTransaction();
       transaction.registerSynchronization(synchronization);
   }

   public boolean isTxInProgress()
   {
      return eventSource.isTransactionInProgress();
   }

}
