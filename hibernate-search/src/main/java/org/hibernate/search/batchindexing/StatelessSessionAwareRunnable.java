package org.hibernate.search.batchindexing;

import org.hibernate.StatelessSession;

/**
 * @author Emmanuel Bernard
 */
public interface StatelessSessionAwareRunnable {
	public void run(StatelessSession session);
}
