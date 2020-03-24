/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.lucene.resources.impl.BackendThreads;
import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.reporting.FailureHandler;

public class LuceneSerialWorkOrchestratorImpl
		extends AbstractWorkOrchestrator<BatchedWork<LuceneBatchedWorkProcessor>>
		implements LuceneSerialWorkOrchestrator {

	// TODO HSEARCH‌-3575 allow to configure this value
	private static final int MAX_WORKS_PER_BATCH = 1000;

	private final LuceneBatchedWorkProcessor processor;
	private final BackendThreads threads;
	private final BatchingExecutor<LuceneBatchedWorkProcessor> executor;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param processor A processor to use in the background thread.
	 * @param threads The threads for this backend.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	public LuceneSerialWorkOrchestratorImpl(
			String name, LuceneBatchedWorkProcessor processor,
			BackendThreads threads,
			FailureHandler failureHandler) {
		super( name );
		this.processor = processor;
		this.threads = threads;
		this.executor = new BatchingExecutor<>(
				name,
				processor,
				MAX_WORKS_PER_BATCH,
				true,
				failureHandler
		);
	}

	@Override
	public void forceCommitInCurrentThread() {
		processor.forceCommit();
	}

	@Override
	public void forceRefreshInCurrentThread() {
		processor.forceRefresh();
	}

	@Override
	protected void doStart() {
		executor.start( threads.getWriteExecutor() );
	}

	@Override
	protected void doSubmit(BatchedWork<LuceneBatchedWorkProcessor> work) throws InterruptedException {
		executor.submit( work );
	}

	@Override
	protected CompletableFuture<?> getCompletion() {
		return executor.getCompletion();
	}

	@Override
	protected void doStop() {
		executor.stop();
	}

}
