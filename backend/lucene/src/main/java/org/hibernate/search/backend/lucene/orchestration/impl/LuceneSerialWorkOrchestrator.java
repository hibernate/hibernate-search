/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;

/**
 * An orchestrator that batches together works sent from other threads.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows processing multiple works in the order they were submitted and only committing once,
 * potentially reducing the frequency of commits.
 */
public interface LuceneSerialWorkOrchestrator {

	default <T> CompletableFuture<T> submit(IndexingWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneBatchedWork<>( work, future ) );
		return future;
	}

	default <T> void submit(CompletableFuture<T> future, IndexingWork<T> work) {
		submit( new LuceneBatchedWork<>( work, future ) );
	}

	void submit(BatchedWork<LuceneBatchedWorkProcessor> work);

	/**
	 * Force a commit immediately.
	 * <p>
	 * The commit will be executed <strong>in the current thread</strong>.
	 */
	void forceCommitInCurrentThread();

	/**
	 * Force a refresh immediately.
	 * <p>
	 * The refresh will be executed <strong>in the current thread</strong>.
	 */
	void forceRefreshInCurrentThread();

}
