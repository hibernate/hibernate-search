/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.Throwables;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Executes single or multiple {@link ElasticsearchWork}s against the Elasticsearch server.
 * <p>
 * When processing multiple requests, bulk requests will be formed and executed as far as possible.
 * <p>
 * Requests can be processed synchronously or asynchronously.
 * In the latter case, incoming requests are added to a queue
 * via a {@link BatchingSharedElasticsearchWorkOrchestrator} and processed in bulks.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchWorkProcessor implements AutoCloseable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ErrorHandler errorHandler;
	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;
	private final ElasticsearchWorkFactory workFactory;

	private final ElasticsearchWorkExecutionContext parallelWorkExecutionContext;
	private final BatchingSharedElasticsearchWorkOrchestrator asyncOrchestrator;

	public ElasticsearchWorkProcessor(BuildContext context,
			ElasticsearchClient client, GsonProvider gsonProvider, ElasticsearchWorkFactory workFactory) {
		this.errorHandler = context.getErrorHandler();
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.workFactory = workFactory;

		this.parallelWorkExecutionContext =
				new ParallelWorkExecutionContext( client, gsonProvider );
		this.asyncOrchestrator = createBatchingSharedOrchestrator( "Elasticsearch async work orchestrator", createSerialOrchestrator() );
	}

	@Override
	public void close() {
		awaitProcessingCompletion( asyncOrchestrator );
		asyncOrchestrator.close();
	}

	/**
	 * Execute a single work synchronously,
	 * potentially throwing exceptions (the error handler isn't used).
	 *
	 * @param work The work to be executed.
	 * @return The result of the given work.
	 */
	public <T> T executeSyncUnsafe(ElasticsearchWork<T> work) {
		try {
			// Note: timeout is handled by the client, so this "join" will not last forever
			return executeAsyncUnsafe( work ).join();
		}
		catch (CompletionException e) {
			throw Throwables.expectRuntimeException( e.getCause() );
		}
	}


	/**
	 * Execute a set of works synchronously.
	 * <p>
	 * Works submitted in the same list will be executed in the given order.
	 * <p>
	 * If any work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning at least the given works,
	 * and the remaining works will not be executed.
	 *
	 * @param works The works to be executed.
	 */
	public void executeSyncSafe(Iterable<ElasticsearchWork<?>> works) {
		FlushableElasticsearchWorkOrchestrator orchestrator = this.createSerialOrchestrator();
		orchestrator.submit( works );
		orchestrator.flush()
				// Note: timeout is handled by the client, so this "join" will not last forever
				.join();
	}

	/**
	 * Execute a single work asynchronously,
	 * without bulking it with other asynchronous works,
	 * and potentially throwing exceptions (the error handler isn't used).
	 *
	 * @param work The work to be executed.
	 * @return The result of the given work.
	 */
	public <T> CompletableFuture<T> executeAsyncUnsafe(ElasticsearchWork<T> work) {
		return start( work, parallelWorkExecutionContext );
	}

	/**
	 * Execute a single work asynchronously,
	 * potentially bulking it with other asynchronous works.
	 * <p>
	 * If the work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning at least this work.
	 *
	 * @param work The work to be executed.
	 */
	public void executeAsync(ElasticsearchWork<?> work) {
		asyncOrchestrator.submit( Collections.singleton( work ) );
	}

	/**
	 * Execute a set of works asynchronously,
	 * potentially bulking it with other asynchronous works.
	 * <p>
	 * Works submitted in the same list will be executed in the given order.
	 * <p>
	 * If any work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning at least the given works,
	 * and the remaining works will not be executed.
	 *
	 * @param works The works to be executed.
	 */
	public void executeAsync(List<ElasticsearchWork<?>> works) {
		asyncOrchestrator.submit( works );
	}

	/**
	 * Blocks until the queue of requests scheduled for asynchronous processing has been fully processed.
	 * N.B. if more work is added to the queue in the meantime, this might delay the wait.
	 */
	public void awaitAsyncProcessingCompletion() {
		awaitProcessingCompletion( asyncOrchestrator );
	}

	private void awaitProcessingCompletion(BatchingSharedElasticsearchWorkOrchestrator orchestrator) {
		try {
			asyncOrchestrator.awaitCompletion();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw LOG.interruptedWhileWaitingForRequestCompletion( e );
		}
	}

	private <T> CompletableFuture<T> start(ElasticsearchWork<T> work, ElasticsearchWorkExecutionContext context) {
		LOG.tracef( "Processing %s", work );
		return work.execute( context );
	}

	private BatchingSharedElasticsearchWorkOrchestrator createBatchingSharedOrchestrator(String name, FlushableElasticsearchWorkOrchestrator delegate) {
		return new BatchingSharedElasticsearchWorkOrchestrator( name, delegate, errorHandler );
	}

	private FlushableElasticsearchWorkOrchestrator createSerialOrchestrator() {
		ElasticsearchWorkSequenceBuilder sequenceBuilder = createSequenceBuilder();
		ElasticsearchWorkBulker bulker = createBulker( sequenceBuilder, true );
		return new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilder, bulker );
	}

	private ElasticsearchWorkSequenceBuilder createSequenceBuilder() {
		return new DefaultElasticsearchWorkSequenceBuilder(
				this::start,
				() -> new SequentialWorkExecutionContext(
						client, gsonProvider, workFactory, ElasticsearchWorkProcessor.this, errorHandler ),
				() -> new DefaultContextualErrorHandler( errorHandler )
				);
	}

	private ElasticsearchWorkBulker createBulker(ElasticsearchWorkSequenceBuilder sequenceBuilder, boolean refreshInBulkAPICall) {
		return new DefaultElasticsearchWorkBulker(
				sequenceBuilder,
				worksToBulk -> workFactory.bulk( worksToBulk ).refresh( refreshInBulkAPICall ).build()
				);
	}

}
