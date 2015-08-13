/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;


import java.util.Set;

import org.hibernate.search.backend.LuceneWork;

/**
 * Implementations of this interface are not drop-in replacements for the standard BackendQueueProcessor,
 * but are meant to be used only during batch processing.
 * The order of LuceneWork(s) processed is not guaranteed as the queue is consumed by several
 * concurrent workers.
 *
 * @author Sanne Grinovero
 */
public interface BatchBackend {

	/**
	 * Enqueues one work to be processed asynchronously
	 *
	 * @param work a {@link org.hibernate.search.backend.LuceneWork} object.
	 * @throws java.lang.InterruptedException if the current thread is interrupted while
	 *                              waiting for the work queue to have enough space.
	 */
	void enqueueAsyncWork(LuceneWork work) throws InterruptedException;

	/**
	 * Does one work in sync
	 *
	 * @param work the lucene work to execute
	 */
	void doWorkInSync(LuceneWork work);

	/**
	 * Since most work is done async in the backend, we need to flush at the end to
	 * make sure we don't return control before all work was processed,
	 * and that IndexWriters are committed or closed.
	 *
	 * @param indexedRootTypes a {@link java.util.Set} object.
	 */
	void flush(Set<Class<?>> indexedRootTypes);

	/**
	 * Triggers optimization of all indexes containing at least one instance of the
	 * listed targetedClasses.
	 *
	 * @param targetedClasses Used to specify which indexes need optimization.
	 */
	void optimize(Set<Class<?>> targetedClasses);

}
