/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.search.backend.impl;

import javax.transaction.Synchronization;

import org.hibernate.search.backend.spi.Work;

/**
 * Represents the synchronization work done when a transaction is committed or rollbacked.
 *
 * @author Emmanuel Bernard &lt;emmanuel@hibernate.org&gt;
 */
public interface WorkQueueSynchronization extends Synchronization {

	/**
	 * Add some work to the transaction queue.
	 * @param work the work to add
	 */
	void add(Work work);

	/**
	 * Whether or not this queue has been consumed
	 * @return {@code true} if the work has been consumed
	 */
	boolean isConsumed();

	/**
	 * Flush the queue by executing the workload
	 */
	void flushWorks();
}
