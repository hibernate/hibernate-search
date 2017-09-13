/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.event.impl;

import java.io.Serializable;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.hibernate.HibernateException;
import org.hibernate.action.spi.AfterTransactionCompletionProcess;
import org.hibernate.action.spi.BeforeTransactionCompletionProcess;
import org.hibernate.engine.spi.ActionQueue;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.FlushEventListener;
import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;
import org.hibernate.service.Service;

/**
 * Implementation of the transactional context on top of an EventSource (Session)
 *
 * @author Navin Surtani  - navin@surtani.org
 * @author Emmanuel Bernard
 * @author Sanne Grinovero
 */
public class EventSourceTransactionContext implements TransactionContext, Serializable {

	private static final Log log = LoggerFactory.make();

	private final EventSource eventSource;

	//this transient is required to break recursive serialization
	private transient FullTextIndexEventListener flushListener;

	//constructor time is too early to define the value of realTxInProgress,
	//postpone it, otherwise doing
	// " openSession - beginTransaction "
	//will behave as "out of transaction" in the whole session lifespan.
	private boolean realTxInProgress = false;
	private boolean realTxInProgressInitialized = false;

	public EventSourceTransactionContext(EventSource eventSource) {
		this.eventSource = eventSource;
		this.flushListener = getIndexWorkFlushEventListener();
	}

	@Override
	public Object getTransactionIdentifier() {
		if ( isRealTransactionInProgress() ) {
			return eventSource.accessTransaction();
		}
		else {
			return eventSource;
		}
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		if ( isRealTransactionInProgress() ) {
			//use {Before|After}TransactionCompletionProcess instead of registerTransaction because it does not
			//swallow transactions.
			/*
             * HSEARCH-540: the pre process must be both a BeforeTransactionCompletionProcess and a TX Synchronization.
             *
             * In a resource-local tx env, the beforeCommit phase is called after the flush, and prepares work queue.
             * Also, any exceptions that occur during that are propagated (if a Synchronization was used, the exceptions
             * would be eaten).
             *
             * In a JTA env, the before transaction completion is called before the flush, so not all changes are yet
             * written. However, Synchronization-s do propagate exceptions, so they can be safely used.
             */
			final ActionQueue actionQueue = eventSource.getActionQueue();
			boolean isLocal = isLocalTransaction();
			if ( isLocal ) {
				//if local tx never use Synchronization
				actionQueue.registerProcess( new DelegateToSynchronizationOnBeforeTx( synchronization ) );
			}
			else {
				//TODO could we remove the action queue registration in this case?
				actionQueue.registerProcess( new DelegateToSynchronizationOnBeforeTx( synchronization ) );
				eventSource.accessTransaction().registerSynchronization(
						new BeforeCommitSynchronizationDelegator( synchronization )
				);
			}

			//executed in all environments
			actionQueue.registerProcess( new DelegateToSynchronizationOnAfterTx( synchronization ) );
		}
		else {
			//registerSynchronization is only called if isRealTransactionInProgress or if
			// a flushListener was found; still we might need to find the listener again
			// as it might have been cleared by serialization (is transient).
			flushListener = getIndexWorkFlushEventListener();
			if ( flushListener != null ) {
				flushListener.addSynchronization( eventSource, synchronization );
			}
			else {
				//shouldn't happen if the code about serialization is fine:
				throw new SearchException( "AssertionFailure: flushListener not registered any more." );
			}
		}
	}

	private boolean isLocalTransaction() {
		return !eventSource
			.getTransactionCoordinator()
			.getTransactionCoordinatorBuilder()
			.isJta();
	}

	private <T extends Service> T getService(Class<T> serviceClass) {
		return eventSource.getFactory().getServiceRegistry().getService( serviceClass );
	}

	private FullTextIndexEventListener getIndexWorkFlushEventListener() {
		if ( this.flushListener != null ) {
			//for the "transient" case: might have been nullified.
			return flushListener;
		}
		final Iterable<FlushEventListener> listeners = getService( EventListenerRegistry.class )
				.getEventListenerGroup( EventType.FLUSH ).listeners();
		for ( FlushEventListener listener : listeners ) {
			if ( FullTextIndexEventListener.class.isAssignableFrom( listener.getClass() ) ) {
				return (FullTextIndexEventListener) listener;
			}
		}
		log.debug( "FullTextIndexEventListener was not registered as FlushEventListener" );
		return null;
	}

	//The code is not really fitting the method name;
	//(unless you consider a flush as a mini-transaction)
	//This is because we want to behave as "inTransaction" if the flushListener is registered.
	@Override
	public boolean isTransactionInProgress() {
		// either it is a real transaction, or if we are capable to manage this in the IndexWorkFlushEventListener
		return getIndexWorkFlushEventListener() != null || isRealTransactionInProgress();
	}

	private boolean isRealTransactionInProgress() {
		if ( ! realTxInProgressInitialized ) {
			realTxInProgress = eventSource.isTransactionInProgress();
			realTxInProgressInitialized = true;
		}
		return realTxInProgress;
	}

	private static class DelegateToSynchronizationOnBeforeTx implements BeforeTransactionCompletionProcess {
		private final Synchronization synchronization;

		DelegateToSynchronizationOnBeforeTx(Synchronization synchronization) {
			this.synchronization = synchronization;
		}

		@Override
		public void doBeforeTransactionCompletion(SessionImplementor sessionImplementor) {
			try {
				synchronization.beforeCompletion();
			}
			catch (Exception e) {
				throw new HibernateException( "Error while indexing in Hibernate Search (before transaction completion)", e );
			}
		}
	}

	private static class DelegateToSynchronizationOnAfterTx implements AfterTransactionCompletionProcess {
		private final Synchronization synchronization;

		DelegateToSynchronizationOnAfterTx(Synchronization synchronization) {
			this.synchronization = synchronization;
		}

		@Override
		public void doAfterTransactionCompletion(boolean success, SharedSessionContractImplementor sessionImplementor) {
			try {
				synchronization.afterCompletion( success ? Status.STATUS_COMMITTED : Status.STATUS_ROLLEDBACK );
			}
			catch (Exception e) {
				throw new HibernateException( "Error while indexing in Hibernate Search (after transaction completion)", e );
			}
		}
	}

	private static class BeforeCommitSynchronizationDelegator implements Synchronization {
		private final Synchronization synchronization;

		public BeforeCommitSynchronizationDelegator(Synchronization sync) {
			this.synchronization = sync;
		}

		@Override
		public void beforeCompletion() {
			this.synchronization.beforeCompletion();
		}

		@Override
		public void afterCompletion(int status) {
			//do not delegate
		}
	}

}
