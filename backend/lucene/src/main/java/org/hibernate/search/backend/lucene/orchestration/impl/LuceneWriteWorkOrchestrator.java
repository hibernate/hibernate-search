/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.LuceneIndexManagementWork;
import org.hibernate.search.backend.lucene.work.impl.LuceneWriteWork;


public interface LuceneWriteWorkOrchestrator {

	default <T> CompletableFuture<T> submit(LuceneIndexManagementWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneManagementWorkSet<>( work, future ) );
		return future;
	}

	default <T> CompletableFuture<T> submit(LuceneWriteWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new LuceneSingleWriteWorkSet<>( work, future ) );
		return future;
	}

	default <T> void submit(CompletableFuture<T> future, LuceneWriteWork<T> work) {
		submit( new LuceneSingleWriteWorkSet<>( work, future ) );
	}

	void submit(LuceneWriteWorkSet workSet);

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
