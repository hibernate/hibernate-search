/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.impl.lucene;

import java.util.List;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;


/**
 * Defines the contract for async and synchronous processors to apply
 * batched of indexing work to the index.
 *
 * @author Sanne Grinovero (C) 2014 Red Hat Inc.
 * @since 5.0
 */
interface WorkProcessor {

	/**
	 * Prepare for the queue to be shut down.
	 * Needs to flush pending work and shutdown any internal threads.
	 */
	void shutdown();

	/**
	 * Enqueues a new batch of indexing work to be applied.
	 * @param workList the list of work
	 * @param monitor any optional listener which needs to be notified for the work.
	 */
	void submit(List<LuceneWork> workList, IndexingMonitor monitor);

	/**
	 * Only invoked when some dynamic parameters are reconfigured
	 * @param resources the new instance to be used
	 */
	void updateResources(LuceneBackendResources resources);

}
