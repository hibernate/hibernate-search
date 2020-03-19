/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.IndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.WriteWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;


public interface LuceneWriteWorkOrchestrator {

	default <T> CompletableFuture<T> submit(IndexManagementWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( future, work );
		return future;
	}

	default <T> void submit(CompletableFuture<T> future, IndexManagementWork<T> work) {
		submit( new LuceneManagementBatchedWork<>( work, future ) );
	}

	default <T> CompletableFuture<T> submit(WriteWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneWriteBatchedWork<>( work, future ) );
		return future;
	}

	default <T> void submit(CompletableFuture<T> future, WriteWork<T> work) {
		submit( new LuceneWriteBatchedWork<>( work, future ) );
	}

	void submit(BatchedWork<LuceneWriteWorkProcessor> work);

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
