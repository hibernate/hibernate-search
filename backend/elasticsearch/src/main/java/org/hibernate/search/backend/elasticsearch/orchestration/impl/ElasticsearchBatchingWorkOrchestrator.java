/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import org.hibernate.search.engine.backend.orchestration.spi.BatchedWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.environment.thread.spi.ThreadPoolProvider;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.impl.Closer;

/**
 * An orchestrator sharing context across multiple threads,
 * allowing works from different threads to be executed in a single thread,
 * thus producing bigger bulk works.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows processing more works when orchestrating, which allows using bulk works
 * more extensively.
 */
class ElasticsearchBatchingWorkOrchestrator extends AbstractElasticsearchWorkOrchestrator
		implements ElasticsearchWorkOrchestratorImplementor {

	private final ThreadPoolProvider threadPoolProvider;
	private ExecutorService executorService;
	private final BatchingExecutor<ElasticsearchWorkProcessor> executor;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param processor A work processor to use in the background thread.
	 * @param threadPoolProvider A provider of thread pools.
	 * @param maxWorksPerBatch The maximum number of works to
	 * process in a single batch. Higher values mean lesser chance of transport
	 * thread starvation, but higher heap consumption.
	 * @param fair if {@code true} works are always submitted to the
	 * delegate in FIFO order, if {@code false} works submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	ElasticsearchBatchingWorkOrchestrator(
			String name, ElasticsearchWorkProcessor processor, ThreadPoolProvider threadPoolProvider,
			int maxWorksPerBatch, boolean fair,
			FailureHandler failureHandler) {
		super( name );
		this.threadPoolProvider = threadPoolProvider;
		this.executor = new BatchingExecutor<>(
				name, processor, maxWorksPerBatch, fair,
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
		executorService = threadPoolProvider.newFixedThreadPool( 1, getName() );
		executor.start( executorService );
	}

	@Override
	protected void doSubmit(BatchedWork<ElasticsearchWorkProcessor> work) throws InterruptedException {
		executor.submit( work );
	}

	@Override
	protected CompletableFuture<?> getCompletion() {
		return executor.getCompletion();
	}

	@Override
	protected void doStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( BatchingExecutor::stop, executor );
			closer.push( ExecutorService::shutdownNow, executorService );
		}
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
		protected void doSubmit(BatchedWork<ElasticsearchWorkProcessor> work) {
			ElasticsearchBatchingWorkOrchestrator.this.submit( work );
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
