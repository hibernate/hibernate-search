/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

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
	private final ElasticsearchWorkOrchestrator syncNonStreamOrchestrator;
	private final BatchingSharedElasticsearchWorkOrchestrator asyncNonStreamOrchestrator;
	private final BatchingSharedElasticsearchWorkOrchestrator streamOrchestrator;

	public ElasticsearchWorkProcessor(BuildContext context,
			ElasticsearchClient client, GsonProvider gsonProvider, ElasticsearchWorkFactory workFactory) {
		this.errorHandler = context.getErrorHandler();
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.workFactory = workFactory;

		this.parallelWorkExecutionContext =
				new ImmutableElasticsearchWorkExecutionContext( client, gsonProvider );
		this.syncNonStreamOrchestrator = createIsolatedSharedOrchestrator( () -> this.createSerialOrchestrator() );
		this.asyncNonStreamOrchestrator = createBatchingSharedOrchestrator( "Elasticsearch async non-stream work orchestrator", createSerialOrchestrator() );
		this.streamOrchestrator = createBatchingSharedOrchestrator( "Elasticsearch async stream work orchestrator", createParallelOrchestrator() );
	}

	@Override
	public void close() {
		try {
			asyncNonStreamOrchestrator.awaitCompletion();
			streamOrchestrator.awaitCompletion();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw LOG.interruptedWhileWaitingForRequestCompletion( e );
		}
		finally {
			try {
				asyncNonStreamOrchestrator.close();
			}
			finally {
				streamOrchestrator.close();
			}
		}
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
	 * Return the orchestrator for synchronous, non-stream background works.
	 * <p>
	 * Works submitted in the same changeset will be executed in the given order.
	 * Relative execution order between changesets is undefined.
	 * <p>
	 * Works submitted to this orchestrator
	 * will only be bulked with subsequent works of the same changeset.
	 * <p>
	 * If any work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning exactly the given works,
	 * and the remaining works will not be executed.
	 *
	 * @return the orchestrator for synchronous, non-stream background works.
	 */
	public ElasticsearchWorkOrchestrator getSyncNonStreamOrchestrator() {
		return syncNonStreamOrchestrator;
	}

	/**
	 * Return the orchestrator for asynchronous, non-stream background works.
	 * <p>
	 * Works submitted in the same changeset will be executed in the given order.
	 * Changesets will be executed in the order they are submitted.
	 * <p>
	 * Works submitted to this orchestrator
	 * will only be bulked with subsequent works (possibly of a different changeset).
	 * <p>
	 * If any work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning exactly the given works,
	 * and the remaining works will not be executed.
	 *
	 * @return the orchestrator for asynchronous, non-stream background works.
	 */
	public BarrierElasticsearchWorkOrchestrator getAsyncNonStreamOrchestrator() {
		return asyncNonStreamOrchestrator;
	}

	/**
	 * Return the orchestrator for asynchronous, non-stream background works.
	 * <p>
	 * Works submitted in the same changeset will be executed in the given order.
	 * Relative execution order between changesets is undefined.
	 * <p>
	 * Works submitted to this orchestrator
	 * will only be bulked with subsequent works from the same changeset
	 * or with works from a different changeset.
	 * <p>
	 * If any work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning exactly the given works,
	 * and the remaining works will not be executed.
	 *
	 * @return the orchestrator for stream background works.
	 */
	public BarrierElasticsearchWorkOrchestrator getStreamOrchestrator() {
		return streamOrchestrator;
	}

	private <T> CompletableFuture<T> start(ElasticsearchWork<T> work, ElasticsearchWorkExecutionContext context) {
		LOG.tracef( "Processing %s", work );
		return work.execute( context );
	}

	private IsolatedSharedElasticsearchWorkOrchestrator createIsolatedSharedOrchestrator(Supplier<FlushableElasticsearchWorkOrchestrator> delegateSupplier) {
		return new IsolatedSharedElasticsearchWorkOrchestrator( delegateSupplier );
	}

	private BatchingSharedElasticsearchWorkOrchestrator createBatchingSharedOrchestrator(String name, FlushableElasticsearchWorkOrchestrator delegate) {
		return new BatchingSharedElasticsearchWorkOrchestrator( name, delegate, errorHandler );
	}

	private FlushableElasticsearchWorkOrchestrator createSerialOrchestrator() {
		ElasticsearchWorkSequenceBuilder sequenceBuilder = createSequenceBuilder();
		ElasticsearchWorkBulker bulker = createBulker( sequenceBuilder, true );
		return new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilder, bulker );
	}

	private FlushableElasticsearchWorkOrchestrator createParallelOrchestrator() {
		ElasticsearchWorkSequenceBuilder sequenceBuilder = createSequenceBuilder();
		ElasticsearchWorkBulker bulker = createBulker( sequenceBuilder, false );
		return new ParallelChangesetsElasticsearchWorkOrchestrator( sequenceBuilder, bulker );
	}

	private ElasticsearchWorkSequenceBuilder createSequenceBuilder() {
		return new DefaultElasticsearchWorkSequenceBuilder(
				this::start,
				() -> new BufferingElasticsearchWorkExecutionContext(
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
