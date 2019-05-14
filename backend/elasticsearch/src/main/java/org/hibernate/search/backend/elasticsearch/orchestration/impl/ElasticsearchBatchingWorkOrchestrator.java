/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.common.spi.ErrorHandler;
import org.hibernate.search.util.common.impl.Closer;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;

/**
 * An orchestrator sharing context across multiple threads,
 * allowing to batch together changesets from different threads,
 * and thus to produce bigger bulk works.
 * <p>
 * More precisely, the submitted works are sent to a queue which is processed periodically
 * in a separate thread.
 * This allows to process more works when orchestrating, which allows to use bulk works
 * more extensively.
 *
 * @author Yoann Rodiere
 */
class ElasticsearchBatchingWorkOrchestrator extends AbstractElasticsearchWorkOrchestrator
		implements ElasticsearchWorkOrchestratorImplementor, AutoCloseable {

	private static final Log log = LoggerFactory.make( Log.class, MethodHandles.lookup() );

	private final BatchingExecutor<ElasticsearchChangeset, ElasticsearchWorkOrchestrationStrategy> executor;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param strategy An orchestration strategy to use in the background thread.
	 * @param maxChangesetsPerBatch The maximum number of changesets to
	 * process in a single batch. Higher values mean lesser chance of transport
	 * thread starvation, but higher heap consumption.
	 * @param fair if {@code true} changesets are always submitted to the
	 * delegate in FIFO order, if {@code false} changesets submitted
	 * when the internal queue is full may be submitted out of order.
	 * @param errorHandler An error handler to report failures of the background thread.
	 */
	public ElasticsearchBatchingWorkOrchestrator(
			String name, ElasticsearchWorkOrchestrationStrategy strategy,
			int maxChangesetsPerBatch, boolean fair,
			ErrorHandler errorHandler) {
		super( name );
		this.executor = new BatchingExecutor<>(
				name, strategy, maxChangesetsPerBatch, fair,
				errorHandler
		);
	}

	@Override
	public void start() {
		executor.start();
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
		return new ChildOrchestrator( name );
	}

	@Override
	protected void doSubmit(ElasticsearchChangeset changeset) throws InterruptedException {
		executor.submit( changeset );
	}

	@Override
	protected void doClose() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.push( ElasticsearchBatchingWorkOrchestrator::awaitCompletionBeforeClose, this );
			closer.push( BatchingExecutor::stop, executor );
		}
	}

	private void awaitCompletionBeforeClose() {
		try {
			executor.awaitCompletion();
		}
		catch (InterruptedException e) {
			log.interruptedWhileWaitingForIndexActivity( getName(), e );
			Thread.currentThread().interrupt();
		}
	}

	private class ChildOrchestrator extends AbstractElasticsearchWorkOrchestrator
			implements ElasticsearchWorkOrchestratorImplementor {

		protected ChildOrchestrator(String name) {
			super( name );
		}

		@Override
		public void start() {
			// uses the resources of the parent orchestrator
		}

		@Override
		protected void doSubmit(ElasticsearchChangeset changeset) {
			ElasticsearchBatchingWorkOrchestrator.this.submit( changeset );
		}

		@Override
		protected void doClose() {
			/*
			 * TODO HSEARCH-3576 this will wait for *all* tasks to finish, including tasks from other children.
			 *  We should do better, see BatchingExecutor.awaitCompletion.
			 */
			ElasticsearchBatchingWorkOrchestrator.this.awaitCompletionBeforeClose();
		}
	}

}
