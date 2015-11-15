/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.manualsource.backend.impl;

import java.io.Serializable;
import javax.transaction.Synchronization;

import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.manualsource.impl.WorkLoadImpl;
import org.hibernate.search.util.logging.impl.Log;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Batch operation per context manually bounded by an explicit API call from users.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public class BatchTransactionContext implements TransactionContext, Serializable {


	private static final Log log = LoggerFactory.make();

	private final WorkLoadImpl eventSource;

	//this transient is required to break recursive serialization
	//private transient FullTextIndexEventListener flushListener;

	//constructor time is too early to define the value of realTxInProgress,
	//postpone it, otherwise doing
	// " openSession - beginTransaction "
	//will behave as "out of transaction" in the whole session lifespan.
	private boolean realTxInProgress = false;
	private boolean realTxInProgressInitialized = false;

	public BatchTransactionContext(WorkLoadImpl eventSource) {
		this.eventSource = eventSource;
	}

	//still needed?
	@Override
	public Object getTransactionIdentifier() {
		return eventSource;
	}

	@Override
	public boolean isTransactionInProgress() {
		// either it is a real transaction, or if we are capable to manage this in the IndexWorkFlushEventListener
		return isRealTransactionInProgress();
	}

	@Override
	public void registerSynchronization(Synchronization synchronization) {
		eventSource.registerSynchronization( synchronization );
	}

	private boolean isRealTransactionInProgress() {
		if ( !realTxInProgressInitialized ) {
			realTxInProgress = eventSource.isTransactionInProgress();
			realTxInProgressInitialized = true;
		}
		return realTxInProgress;
	}
}
