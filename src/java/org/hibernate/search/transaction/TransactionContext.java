package org.hibernate.search.transaction;

import javax.transaction.Synchronization;
import java.io.Serializable;

/**
@author Navin Surtani  - navin@surtani.org
 */
public interface TransactionContext extends Serializable
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
