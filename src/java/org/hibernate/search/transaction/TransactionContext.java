package org.hibernate.search.transaction;

import javax.transaction.Synchronization;

/**
@author Navin Surtani  - navin@surtani.org
 */
public interface TransactionContext
{
   /**
    *@return A boolean whether a transaction is in progress or not.
    */
   public boolean isTxInProgress();

   /**
    *
    * @return  a transaction object.
    */
   public Object getTransactionIdentifier();

   public void registerSynchronization(Synchronization synchronization);
}
