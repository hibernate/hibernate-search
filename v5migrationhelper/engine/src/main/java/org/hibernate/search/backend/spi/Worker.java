/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

import java.util.Properties;

import org.hibernate.search.backend.TransactionContext;
import org.hibernate.search.backend.impl.QueueingProcessor;
import org.hibernate.search.spi.WorkerBuildContext;

/**
 * Perform work for a given context (eg a transaction). This implementation has to be threaded-safe.
 *
 * @author Emmanuel Bernard
 */
public interface Worker {
	/**
	 * Declare a work to be done within a given transaction context
	 *
	 * @param work the work to be executed
	 * @param transactionContext transactional context information
	 */
	void performWork(Work work, TransactionContext transactionContext);

	void initialize(Properties props, WorkerBuildContext context, QueueingProcessor queueingProcessor);

	/**
	 * clean resources
	 * This method can return exceptions
	 */
	void close();

	/**
	 * Flush any work queue.
	 *
	 * @param transactionContext the current transaction (context).
	 */
	void flushWorks(TransactionContext transactionContext);
}
