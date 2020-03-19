/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

/**
 * A work ready to be submitted to a {@link BatchingExecutor},
 * and eventually processed in a batch by a {@link BatchedWorkProcessor}.
 *
 * @param <P> The type of processor this work can be submitted to.
 *
 * @see BatchingExecutor
 */
public interface BatchedWork<P> {

	void submitTo(P processor);

	void markAsFailed(Throwable t);

}
