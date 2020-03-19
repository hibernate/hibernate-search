/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.backend.orchestration.spi;

import java.util.concurrent.CompletableFuture;

/**
 * A processor of batched works submitted to a {@link BatchingExecutor}.
 *
 * @see BatchingExecutor
 */
public interface BatchedWorkProcessor {

	/**
	 * Initializes internal state before works are submitted.
	 */
	void beginBatch();

	/**
	 * Ensures all works submitted
	 * since the last call to {@link #beginBatch()}
	 * will actually be executed, along with any finishing task (commit, ...).
	 *
	 * @return A future completing when the executor is allowed to start another batch.
	 */
	CompletableFuture<?> endBatch();

	/**
	 * Executes any outstanding operation if possible, or return an estimation of when they can be executed.
	 * <p>
	 * Called when the executor considers the work queue complete
	 * and does not plan on submitting another batch due to work starvation.
	 *
	 * @return {@code 0} if there is no outstanding operation, or a positive number of milliseconds
	 * if there are outstanding operations and {@link #completeOrDelay()}
	 * must be called again that many milliseconds later.
	 */
	long completeOrDelay();

}
