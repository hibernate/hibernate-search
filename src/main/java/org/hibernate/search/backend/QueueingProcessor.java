//$Id$
package org.hibernate.search.backend;

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
	 */
	void add(Work work, WorkQueue workQueue);

	/**
	 * prepare resources for a later performWorks call
	 */
	void prepareWorks(WorkQueue workQueue);

	/**
	 * Execute works
	 */
	void performWorks(WorkQueue workQueue);

	/**
	 * Rollback works
	 */
	void cancelWorks(WorkQueue workQueue);

	/**
	 * clean resources
	 * This method should log errors rather than raise an exception
	 */
	void close();
}
