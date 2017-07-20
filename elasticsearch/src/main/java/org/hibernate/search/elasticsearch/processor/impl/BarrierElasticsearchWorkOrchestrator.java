/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

/**
 * An {@link ElasticsearchWorkOrchestrator} providing a synchronization barrier
 * through its {@link #awaitCompletion()} method.
 *
 * @author Yoann Rodiere
 */
public interface BarrierElasticsearchWorkOrchestrator extends ElasticsearchWorkOrchestrator, AutoCloseable {

	/**
	 * Block until there is no more work to execute.
	 * <p>
	 * N.B. if more works are submitted in the meantime, this might delay the wait.
	 *
	 * @throws InterruptedException if thread interrupted while waiting
	 */
	void awaitCompletion() throws InterruptedException;

	@Override
	void close();

}
