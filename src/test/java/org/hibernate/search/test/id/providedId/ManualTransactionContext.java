package org.hibernate.search.test.id.providedId;

import java.util.List;
import java.util.ArrayList;
import javax.transaction.Synchronization;
import javax.transaction.Status;

import org.hibernate.search.backend.TransactionContext;

/**
 * @author Emmanuel Bernard
 */
public class ManualTransactionContext implements TransactionContext {
	private boolean progress = true;
	private List<Synchronization> syncs = new ArrayList<Synchronization>();

	public boolean isTransactionInProgress() {
		return progress;
	}

	public Object getTransactionIdentifier() {
		return this;
	}

	public void registerSynchronization(Synchronization synchronization) {
		syncs.add(synchronization);
	}

	public void end() {
		this.progress = false;
		for (Synchronization sync : syncs) {
			sync.beforeCompletion();
		}

		for (Synchronization sync : syncs) {
			sync.afterCompletion( Status.STATUS_COMMITTED );
		}
	}
}
