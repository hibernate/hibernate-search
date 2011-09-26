package org.hibernate.search.batchindexing.impl;

import org.hibernate.Session;

/**
 * @author Emmanuel Bernard
 */
public interface SessionAwareRunnable {
	public void run(Session session);
}
