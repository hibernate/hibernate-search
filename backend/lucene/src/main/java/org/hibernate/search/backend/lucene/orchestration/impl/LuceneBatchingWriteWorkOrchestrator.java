/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.orchestration.spi.AbstractWorkOrchestrator;
import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;

/**
 * An orchestrator that batches together works sent from other threads.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows processing multiple works and only committing once,
 * potentially reducing the frequency of commits.
 */
public class LuceneBatchingWriteWorkOrchestrator
		extends AbstractWorkOrchestrator<BatchedWork<LuceneWriteWorkProcessor>>
		implements LuceneWriteWorkOrchestratorImplementor {

	// TODO HSEARCH‌-3575 allow to configure this value
	private static final int MAX_WORKS_PER_BATCH = 1000;

	private final LuceneWriteWorkProcessor processor;
	private final ThreadPoolProvider threadPoolProvider;
	private final BatchingExecutor<LuceneWriteWorkProcessor> executor;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param processor A processor to use in the background thread.
	 * @param threadPoolProvider A provider of thread pools.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	public LuceneBatchingWriteWorkOrchestrator(
			String name, LuceneWriteWorkProcessor processor,
			ThreadPoolProvider threadPoolProvider,
			FailureHandler failureHandler) {
		super( name );
		this.processor = processor;
		this.threadPoolProvider = threadPoolProvider;
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
		executor.start( threadPoolProvider );
	}

	@Override
	protected void doSubmit(BatchedWork<LuceneWriteWorkProcessor> work) throws InterruptedException {
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
