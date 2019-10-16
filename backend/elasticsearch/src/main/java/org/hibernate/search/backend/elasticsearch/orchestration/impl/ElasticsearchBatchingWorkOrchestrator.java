/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;

/**
 * An orchestrator sharing context across multiple threads,
 * allowing to batch together worksets from different threads,
 * and thus to produce bigger bulk works.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows to process more works when orchestrating, which allows to use bulk works
 * more extensively.
 *
 */
class ElasticsearchBatchingWorkOrchestrator extends AbstractElasticsearchWorkOrchestrator
		implements ElasticsearchWorkOrchestratorImplementor {

	private final ThreadPoolProvider threadPoolProvider;
	private final BatchingExecutor<ElasticsearchWorkSet, ElasticsearchWorkProcessor> executor;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param processor A work processor to use in the background thread.
	 * @param threadPoolProvider A provider of thread pools.
	 * @param maxWorksetsPerBatch The maximum number of worksets to
	 * process in a single batch. Higher values mean lesser chance of transport
	 * thread starvation, but higher heap consumption.
	 * @param fair if {@code true} worksets are always submitted to the
	 * delegate in FIFO order, if {@code false} worksets submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	ElasticsearchBatchingWorkOrchestrator(
			String name, ElasticsearchWorkProcessor processor, ThreadPoolProvider threadPoolProvider,
			int maxWorksetsPerBatch, boolean fair,
			FailureHandler failureHandler) {
		super( name );
		this.threadPoolProvider = threadPoolProvider;
		this.executor = new BatchingExecutor<>(
				name, processor, maxWorksetsPerBatch, fair,
				failureHandler
		);
	}

	/**
	 * Create a child orchestrator.
	 * <p>
	 * The child orchestrator will use the same resources and its works
	 * will be executed by the same background thread.
	 * <p>
	 * Closing the child will not close the parent,
	 * but will make the current thread wait for the completion of previously submitted works,
	 * and will prevent any more work to be submitted through the child.
	 *
	 * @param name The name of the child orchestrator when reporting errors
	 */
	public ElasticsearchWorkOrchestratorImplementor createChild(String name) {
		return new ElasticsearchChildBatchingWorkOrchestrator( name );
	}

	@Override
	protected void doStart() {
		executor.start( threadPoolProvider );
	}

	@Override
	protected void doSubmit(ElasticsearchWorkSet workSet) throws InterruptedException {
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

	private class ElasticsearchChildBatchingWorkOrchestrator extends AbstractElasticsearchWorkOrchestrator
			implements ElasticsearchWorkOrchestratorImplementor {

		protected ElasticsearchChildBatchingWorkOrchestrator(String name) {
			super( name );
		}

		@Override
		protected void doStart() {
			// uses the resources of the parent orchestrator
		}

		@Override
		protected void doSubmit(ElasticsearchWorkSet workSet) {
			ElasticsearchBatchingWorkOrchestrator.this.submit( workSet );
		}

		@Override
		protected CompletableFuture<?> getCompletion() {
			/*
			 * TODO HSEARCH-3576 this will wait for *all* tasks to finish, including tasks from other children.
			 *  We should do better.
			 */
			return ElasticsearchBatchingWorkOrchestrator.this.getCompletion();
		}

		@Override
		protected void doStop() {
			// uses the resources of the parent orchestrator
		}
	}

}
