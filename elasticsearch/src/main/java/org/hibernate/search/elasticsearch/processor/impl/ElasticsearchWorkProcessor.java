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

	private static final int NON_STREAM_MIN_BULK_SIZE = 2;
	/*
	 * For stream works, we use a minimum bulk size of 1,
	 * and thus allow bulks with only one work.
	 * The reason is, for stream works, we only submit single-work changesets,
	 * which means the decision on whether to bulk the work or not will always happen
	 * immediately after each work, when we only have one work to bulk.
	 * Thus if we set the minimum to a value higher than 1, we would always
	 * decide not to start a bulk (because there would always be only one
	 * work to bulk), which would result in terrible performance.
	 */
	private static final int STREAM_MIN_BULK_SIZE = 1;
	private static final int MAX_BULK_SIZE = 250;

	/*
	 * Setting the following constants involves a bit of guesswork.
	 * Basically we want the number to be large enough for the orchestrator
	 * to create bulks of the maximum size defined above most of the time,
	 * but we also want to keep the number as low as possible to avoid
	 * consuming too much memory with pending changesets.
	 * Here we set the number for stream works higher than the number
	 * for non-stream works, because stream works will only ever be grouped
	 * in single-work changesets, and also because the stream work
	 * orchestrator is shared between all index managers.
	 */
	private static final int NON_STREAM_MAX_CHANGESETS_PER_BATCH = 10 * MAX_BULK_SIZE;
	private static final int STREAM_MAX_CHANGESETS_PER_BATCH = 20 * MAX_BULK_SIZE;

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final ErrorHandler errorHandler;
	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;
	private final ElasticsearchWorkFactory workFactory;

	private final ElasticsearchWorkExecutionContext parallelWorkExecutionContext;
	private final BarrierElasticsearchWorkOrchestrator streamOrchestrator;

	public ElasticsearchWorkProcessor(BuildContext context,
			ElasticsearchClient client, GsonProvider gsonProvider, ElasticsearchWorkFactory workFactory) {
		this.errorHandler = context.getErrorHandler();
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.workFactory = workFactory;

		this.parallelWorkExecutionContext =
				new ImmutableElasticsearchWorkExecutionContext( client, gsonProvider );

		/*
		 * The following orchestrator doesn't require a strict execution ordering
		 * (because it's mainly used by the mass indexer, which already takes care of
		 * ordering works properly and waiting for pending works when necessary).
		 * Thus we use a parallel orchestrator to maximize throughput.
		 * Also, since works are not applied in order, and since API users have no way
		 * to determine whether a work finished or not, explicit refreshes are useless,
		 * so we disable refreshes both in the bulk API call and in the execution contexts.
		 */
		this.streamOrchestrator = createBatchingSharedOrchestrator(
				"Elasticsearch async stream work orchestrator",
				STREAM_MAX_CHANGESETS_PER_BATCH,
				false, // Do not care about ordering when queuing changesets
				createParallelOrchestrator( this::createIndexMonitorBufferingWorkExecutionContext, STREAM_MIN_BULK_SIZE, false ) );
	}

	@Override
	public void close() {
		try {
			streamOrchestrator.awaitCompletion();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw LOG.interruptedWhileWaitingForRequestCompletion( e );
		}
		finally {
			streamOrchestrator.close();
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
	 * Return an orchestrator for non-stream background works.
	 * <p>
	 * Works submitted in the same changeset will be executed in the given order.
	 * Relative execution order between changesets is undefined.
	 * <p>
	 * Works submitted to this orchestrator
	 * will only be bulked with subsequent works (possibly of a different changeset).
	 * <p>
	 * If any work throws an exception, this exception will be passed
	 * to the error handler with an {@link ErrorContext} spanning exactly the given works,
	 * and the remaining works will not be executed.
	 *
	 * @return the orchestrator for synchronous, non-stream background works.
	 */
	public BarrierElasticsearchWorkOrchestrator createNonStreamOrchestrator(String indexName, boolean refreshAfterWrite) {
		/*
		 * Since works are applied in order, refreshing the index after changesets
		 * is actually an option, and if enabled we use refreshing execution contexts.
		 * In order to reduce the cost of those refreshes, we also try to batch together
		 * refreshes for works bulked in the same bulk API call. Non-bulked works will have
		 * their refresh executed at the end of each changeset.
		 */
		Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplier;
		boolean refreshInBulkApiCall;
		if ( refreshAfterWrite ) {
			contextSupplier = this::createRefreshingWorkExecutionContext;
			refreshInBulkApiCall = true;
		}
		else {
			contextSupplier = this::createIndexMonitorBufferingWorkExecutionContext;
			refreshInBulkApiCall = false;
		}

		/*
		 * The non-stream orchestrator requires a strict execution ordering,
		 * because subsequent changesets may be inter-dependent (an addition in one changeset
		 * followed by a deletion in the next, for instance).
		 * To make sure we apply works in order, we use serial orchestrators.
		 */
		FlushableElasticsearchWorkOrchestrator delegate =
				createSerialOrchestrator( contextSupplier, NON_STREAM_MIN_BULK_SIZE, refreshInBulkApiCall );

		return createBatchingSharedOrchestrator(
				"Elasticsearch non-stream work orchestrator for index " + indexName,
				NON_STREAM_MAX_CHANGESETS_PER_BATCH,
				true /* enqueue changesets in the order they were submitted */,
				delegate
				);
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

	private BatchingSharedElasticsearchWorkOrchestrator createBatchingSharedOrchestrator(
			String name, int maxChangesetsPerBatch, boolean fair,
			FlushableElasticsearchWorkOrchestrator delegate) {
		return new BatchingSharedElasticsearchWorkOrchestrator( name, maxChangesetsPerBatch, fair,
				delegate, errorHandler );
	}

	private FlushableElasticsearchWorkOrchestrator createSerialOrchestrator(
			Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplier, int minBulkSize, boolean refreshInBulkAPICall) {
		ElasticsearchWorkSequenceBuilder sequenceBuilder = createSequenceBuilder( contextSupplier );
		ElasticsearchWorkBulker bulker = createBulker( sequenceBuilder, minBulkSize, refreshInBulkAPICall );
		return new SerialChangesetsElasticsearchWorkOrchestrator( sequenceBuilder, bulker );
	}

	private FlushableElasticsearchWorkOrchestrator createParallelOrchestrator(
			Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplier, int minBulkSize, boolean refreshInBulkAPICall) {
		ElasticsearchWorkSequenceBuilder sequenceBuilder = createSequenceBuilder( contextSupplier );
		ElasticsearchWorkBulker bulker = createBulker( sequenceBuilder, minBulkSize, refreshInBulkAPICall );
		return new ParallelChangesetsElasticsearchWorkOrchestrator( sequenceBuilder, bulker );
	}

	private ElasticsearchWorkSequenceBuilder createSequenceBuilder(Supplier<FlushableElasticsearchWorkExecutionContext> contextSupplier) {
		return new DefaultElasticsearchWorkSequenceBuilder(
				this::start,
				contextSupplier,
				() -> new DefaultContextualErrorHandler( errorHandler )
				);
	}

	private ElasticsearchWorkBulker createBulker(ElasticsearchWorkSequenceBuilder sequenceBuilder, int minBulkSize, boolean refreshInBulkAPICall) {
		return new DefaultElasticsearchWorkBulker(
				sequenceBuilder,
				worksToBulk -> workFactory.bulk( worksToBulk ).refresh( refreshInBulkAPICall ).build(),
				minBulkSize, MAX_BULK_SIZE
				);
	}

	private FlushableElasticsearchWorkExecutionContext createIndexMonitorBufferingWorkExecutionContext() {
		return new IndexMonitorBufferingElasticsearchWorkExecutionContext( client, gsonProvider, errorHandler );
	}

	private FlushableElasticsearchWorkExecutionContext createRefreshingWorkExecutionContext() {
		return new RefreshingElasticsearchWorkExecutionContext(
				client, gsonProvider, workFactory, ElasticsearchWorkProcessor.this, errorHandler );
	}

}
