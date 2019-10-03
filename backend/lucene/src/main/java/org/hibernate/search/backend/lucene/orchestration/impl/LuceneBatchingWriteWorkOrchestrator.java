/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.reporting.ErrorHandler;

/**
 * An orchestrator that batches together worksets sent from other threads.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows to process multiple worksets and only commit once,
 * potentially reducing the frequency of commits.
 */
public class LuceneBatchingWriteWorkOrchestrator extends AbstractLuceneWriteWorkOrchestrator
		implements LuceneWriteWorkOrchestratorImplementor {

	// TODO HSEARCH‌-3575 allow to configure this value
	private static final int MAX_WORKSETS_PER_BATCH = 1000;

	private final BatchingExecutor<LuceneWorkSet, LuceneWriteWorkProcessor> executor;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param processor A processor to use in the background thread.
	 * @param errorHandler An error handler to report failures of the background thread.
	 */
	public LuceneBatchingWriteWorkOrchestrator(
			String name, LuceneWriteWorkProcessor processor,
			ErrorHandler errorHandler) {
		super( name );
		this.executor = new BatchingExecutor<>(
				name,
				processor,
				MAX_WORKSETS_PER_BATCH,
				true,
				errorHandler
		);
	}

	@Override
	public CompletableFuture<?> ensureIndexExists() {
		CompletableFuture<Object> future = new CompletableFuture<>();
		submit( new LuceneEnsureIndexExistsWorkSet( future ) );
		return future;
	}

	@Override
	protected void doStart() {
		executor.start();
	}

	@Override
	protected void doSubmit(LuceneWorkSet workSet) throws InterruptedException {
		executor.submit( workSet );
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
