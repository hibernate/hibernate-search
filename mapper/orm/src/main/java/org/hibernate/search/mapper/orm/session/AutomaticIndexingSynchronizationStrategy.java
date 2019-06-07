/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.session;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.work.execution.DocumentCommitStrategy;
import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;
import org.hibernate.search.util.common.impl.Futures;

/**
 * Determines how the thread will block upon committing a transaction
 * where indexed entities were modified.
 *
 * @see SearchSession#setAutomaticIndexingSynchronizationStrategy(AutomaticIndexingSynchronizationStrategy)
 */
public interface AutomaticIndexingSynchronizationStrategy {

	/**
	 * @return A strategy describing how commits should be handled after document changes are applied.
	 */
	DocumentCommitStrategy getDocumentCommitStrategy();

	/**
	 * @return A strategy describing how refresh should be handled after document changes are applied.
	 */
	DocumentRefreshStrategy getDocumentRefreshStrategy();

	/**
	 * Handle the result of the (asynchronous) indexing.
	 * <p>
	 * This typically involves waiting on the given future,
	 * to prevent the thread from resuming execution until indexing is complete.
	 *
	 * @param future A future that will be completed once all document changes are applied
	 * and the commit/refresh requirements defined by {@link #getDocumentCommitStrategy()}
	 * and {@link #getDocumentRefreshStrategy()} are satisfied.
	 */
	void handleFuture(CompletableFuture<?> future);

	/**
	 * @return A strategy that only waits for indexing requests to be queued in the backend.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy queued() {
		return new AutomaticIndexingSynchronizationStrategy() {
			@Override
			public DocumentCommitStrategy getDocumentCommitStrategy() {
				return DocumentCommitStrategy.NONE;
			}

			@Override
			public DocumentRefreshStrategy getDocumentRefreshStrategy() {
				return DocumentRefreshStrategy.NONE;
			}

			@Override
			public void handleFuture(CompletableFuture<?> future) {
				// Nothing to do: works are queued, we're fine.
			}
		};
	}

	/**
	 * @return A strategy that waits for indexing requests to be committed.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy committed() {
		return new AutomaticIndexingSynchronizationStrategy() {
			@Override
			public DocumentCommitStrategy getDocumentCommitStrategy() {
				return DocumentCommitStrategy.FORCE;
			}

			@Override
			public DocumentRefreshStrategy getDocumentRefreshStrategy() {
				return DocumentRefreshStrategy.NONE;
			}

			@Override
			public void handleFuture(CompletableFuture<?> future) {
				Futures.unwrappedExceptionJoin( future );
			}
		};
	}

	/**
	 * @return A strategy that waits for indexing requests to be committed and forces index refreshes.
	 * See the reference documentation for details.
	 */
	static AutomaticIndexingSynchronizationStrategy searchable() {
		return new AutomaticIndexingSynchronizationStrategy() {
			@Override
			public DocumentCommitStrategy getDocumentCommitStrategy() {
				return DocumentCommitStrategy.FORCE;
			}

			@Override
			public DocumentRefreshStrategy getDocumentRefreshStrategy() {
				return DocumentRefreshStrategy.FORCE;
			}

			@Override
			public void handleFuture(CompletableFuture<?> future) {
				Futures.unwrappedExceptionJoin( future );
			}
		};
	}

}
