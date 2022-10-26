/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.orchestration.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.cfg.ElasticsearchIndexSettings;
import org.hibernate.search.backend.elasticsearch.link.impl.ElasticsearchLink;
import org.hibernate.search.backend.elasticsearch.resources.impl.BackendThreads;
import org.hibernate.search.backend.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.backend.elasticsearch.work.impl.IndexingWork;
import org.hibernate.search.engine.backend.orchestration.spi.BatchingExecutor;
import org.hibernate.search.engine.backend.work.execution.OperationSubmitter;
import org.hibernate.search.engine.cfg.spi.ConfigurationProperty;
import org.hibernate.search.engine.cfg.ConfigurationPropertySource;
import org.hibernate.search.engine.reporting.FailureHandler;
import org.hibernate.search.util.common.data.impl.HashTable;
import org.hibernate.search.util.common.data.impl.ModuloHashTable;
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

	private static final ConfigurationProperty<Integer> QUEUE_COUNT =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.INDEXING_QUEUE_COUNT )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchIndexSettings.Defaults.INDEXING_QUEUE_COUNT )
					.build();

	private static final ConfigurationProperty<Integer> QUEUE_SIZE =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.INDEXING_QUEUE_SIZE )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchIndexSettings.Defaults.INDEXING_QUEUE_SIZE )
					.build();

	private static final ConfigurationProperty<Integer> MAX_BULK_SIZE =
			ConfigurationProperty.forKey( ElasticsearchIndexSettings.INDEXING_MAX_BULK_SIZE )
					.asIntegerStrictlyPositive()
					.withDefault( ElasticsearchIndexSettings.Defaults.INDEXING_MAX_BULK_SIZE )
					.build();

	private final BackendThreads threads;
	private final FailureHandler failureHandler;

	private HashTable<BatchingExecutor<ElasticsearchBatchedWorkProcessor>> executors;

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
		this.failureHandler = failureHandler;
	}

	@Override
	public <T> CompletableFuture<T> submit(IndexingWork<T> work, OperationSubmitter operationSubmitter) {
		CompletableFuture<T> future = new CompletableFuture<>();
		submit( new ElasticsearchBatchedWork<>( work, future ), operationSubmitter );
		return future;
	}

	@Override
	protected void doStart(ConfigurationPropertySource propertySource) {
		int queueCount = QUEUE_COUNT.get( propertySource );
		int queueSize = QUEUE_SIZE.get( propertySource );
		int maxBulkSize = MAX_BULK_SIZE.get( propertySource );

		ElasticsearchWorkExecutionContext executionContext = createWorkExecutionContext();

		executors = new ModuloHashTable<>( SimpleHashFunction.INSTANCE, queueCount );
		for ( int i = 0; i < executors.size(); i++ ) {
			// Processors are not thread-safe: create one per executor.
			ElasticsearchBatchedWorkProcessor processor = createProcessor( executionContext, maxBulkSize );
			executors.set( i, new BatchingExecutor<>(
					name() + " - " + i,
					processor,
					queueSize,
					true,
					failureHandler
			) );
		}

		for ( BatchingExecutor<?> executor : executors ) {
			executor.start( threads.getWorkExecutor() );
		}
	}

	@Override
	protected void doSubmit(ElasticsearchBatchedWork<?> work, OperationSubmitter operationSubmitter) throws InterruptedException {
		executors.get( work.getQueuingKey() ).submit( work, operationSubmitter );
	}

	@Override
	protected CompletableFuture<?> completion() {
		CompletableFuture<?>[] completions = new CompletableFuture[executors.size()];
		for ( int i = 0; i < executors.size(); i++ ) {
			completions[i] = executors.get( i ).completion();
		}
		return CompletableFuture.allOf( completions );
	}

	@Override
	protected void doStop() {
		try ( Closer<RuntimeException> closer = new Closer<>() ) {
			closer.pushAll( BatchingExecutor::stop, executors );
		}
	}

	private ElasticsearchBatchedWorkProcessor createProcessor(ElasticsearchWorkExecutionContext context,
			int maxBulkSize) {
		ElasticsearchWorkSequenceBuilder sequenceBuilder = new ElasticsearchDefaultWorkSequenceBuilder( context );
		ElasticsearchWorkBulker bulker = new ElasticsearchDefaultWorkBulker(
				sequenceBuilder,
				(worksToBulk, refreshStrategy) ->
						link.getWorkFactory().bulk( worksToBulk ).refresh( refreshStrategy ).build(),
				maxBulkSize
		);
		return new ElasticsearchBatchedWorkProcessor( sequenceBuilder, bulker );
	}

}
