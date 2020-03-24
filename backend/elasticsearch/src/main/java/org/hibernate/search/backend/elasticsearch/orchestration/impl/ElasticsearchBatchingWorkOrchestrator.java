/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.data.impl.SimpleHashFunction;
import org.hibernate.search.util.common.impl.Closer;

/**
 * An orchestrator sending works to a queue which is processed periodically
 * in a separate thread.
 * <p>
 * Works are processed in the order they are submitted.
 * <p>
 * Processing works in a single thread means more works can be processed at a time,
 * which is a good thing when using bulk works.
 */
public class ElasticsearchBatchingWorkOrchestrator
		extends AbstractElasticsearchWorkOrchestrator<ElasticsearchBatchedWork<?>>
		implements ElasticsearchSerialWorkOrchestrator {

	// TODO HSEARCH-3575 make this configurable
	private static final int MAX_BULK_SIZE = 250;
	/*
	 * Setting the following constant involves a bit of guesswork.
	 * Basically we want the number to be large enough for the orchestrator
	 * to create bulks of the maximum size defined above most of the time,
	 * and to avoid cases where the queue is full as much as possible,
	 * because threads submitting works will block when that happens.
	 * But we also want to keep the number as low as possible to avoid
	 * consuming too much memory with pending works.
	 */
	// TODO HSEARCH-3575 make this configurable
	private static final int QUEUE_SIZE = 10 * MAX_BULK_SIZE;
	// TODO HSEARCH-3575 make this configurable
	private static final int PARALLELISM = 10;

	private final BackendThreads threads;
	private final BatchingExecutor<ElasticsearchBatchedWorkProcessor>[] executors;

	/**
	 * @param name The name of the orchestrator thread (and of this orchestrator when reporting errors)
	 * @param threads The threads for this backend.
	 * @param link The Elasticsearch link for this backend.
	 * @param failureHandler A failure handler to report failures of the background thread.
	 */
	public ElasticsearchBatchingWorkOrchestrator(
			String name, BackendThreads threads, ElasticsearchLink link,
			FailureHandler failureHandler) {
		super( name, link );
		this.threads = threads;
		this.executors = new BatchingExecutor[PARALLELISM];
		for ( int i = 0; i < executors.length; i++ ) {
			// Processors are not thread-safe: create one per executor.
			ElasticsearchBatchedWorkProcessor processor = createProcessor();
			executors[i] = new BatchingExecutor<>(
					name + " - " + i,
					processor,
					QUEUE_SIZE,
					true,
					failureHandler
			);
		}
	}

	@Override
	public <T> CompletableFuture<T> submit(IndexingWork<T> work) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchBatchedWork<>( work, future ) );
		return future;
	}

	@Override
	protected void doStart() {
		for ( BatchingExecutor<?> executor : executors ) {
			executor.start( threads.getWorkExecutor() );
		}
	}

	@Override
	protected void doSubmit(ElasticsearchBatchedWork<?> work) throws InterruptedException {
		SimpleHashFunction.pick( executors, work.getQueuingKey() )
				.submit( work );
	}

	@Override
	protected CompletableFuture<?> getCompletion() {
		CompletableFuture<?>[] completions = new CompletableFuture[executors.length];
		for ( int i = 0; i < executors.length; i++ ) {
			completions[i] = executors[i].getCompletion();
		}
		return CompletableFuture.allOf( completions );
	}

	@Override
	protected void doStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BatchingExecutor::stop, executors );
		}
	}

	private ElasticsearchBatchedWorkProcessor createProcessor() {
		ElasticsearchWorkSequenceBuilder sequenceBuilder =
				new ElasticsearchDefaultWorkSequenceBuilder( this::createWorkExecutionContext );
		ElasticsearchWorkBulker bulker = new ElasticsearchDefaultWorkBulker(
				sequenceBuilder,
				(worksToBulk, refreshStrategy) ->
						link.getWorkBuilderFactory().bulk( worksToBulk ).refresh( refreshStrategy ).build(),
				MAX_BULK_SIZE
		);
		return new ElasticsearchBatchedWorkProcessor( sequenceBuilder, bulker );
	}

}
