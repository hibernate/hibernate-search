/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;

/**
 * An orchestrator that batches together works sent from other threads.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows processing multiple works in the order they were submitted and only committing once,
 * potentially reducing the frequency of commits.
 */
public interface LuceneSerialWorkOrchestrator {

	default <T> void submit(CompletableFuture<T> future, IndexingWork<T> work, OperationSubmitter operationSubmitter) {
		submit( new LuceneBatchedWork<>( work, future ), operationSubmitter );
	}

	void submit(LuceneBatchedWork<?> work, OperationSubmitter operationSubmitter);

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
