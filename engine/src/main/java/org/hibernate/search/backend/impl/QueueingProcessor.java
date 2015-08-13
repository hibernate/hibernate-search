/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl;

import org.hibernate.search.backend.spi.Work;

/**
 * Pile work operations
 * No thread safety has to be implemented, the queue being scoped already
 * The implementation must be "stateless" wrt the queue through (ie not store the queue state)
 *
 * FIXME this Interface does not make much sense, since the impl will not be changed
 *
 * @author Emmanuel Bernard
 */
public interface QueueingProcessor {
	/**
	 * Add a work
	 * TODO move that somewhere else, it does not really fit here
	 * @param work the work to add
	 * @param workQueue the work queue
	 */
	void add(Work work, WorkQueue workQueue);

	/**
	 * prepare resources for a later performWorks call
	 * @param workQueue the work queue
	 */
	void prepareWorks(WorkQueue workQueue);

	/**
	 * Execute works
	 * @param workQueue the work queue
	 */
	void performWorks(WorkQueue workQueue);

	/**
	 * Rollback works
	 * @param workQueue the work queue
	 */
	void cancelWorks(WorkQueue workQueue);

}
