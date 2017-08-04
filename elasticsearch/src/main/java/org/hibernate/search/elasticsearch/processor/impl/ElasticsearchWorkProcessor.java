/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.MultiWriteDrainableLinkedList;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchClient;
import org.hibernate.search.elasticsearch.gson.impl.GsonProvider;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.BulkRequestFailedException;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.elasticsearch.work.impl.factory.ElasticsearchWorkFactory;
import org.hibernate.search.exception.ErrorContext;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.impl.Executors;
import org.hibernate.search.util.impl.Futures;
import org.hibernate.search.util.impl.Throwables;
import org.hibernate.search.util.logging.impl.LoggerFactory;

/**
 * Executes single or multiple {@link ElasticsearchWork}s against the Elasticsearch server.
 * <p>
 * When processing multiple requests, bulk requests will be formed and executed as far as possible.
 * <p>
 * Requests can be processed synchronously or asynchronously. In the latter case, incoming requests are added to a queue
 * via {@link AsyncBackendRequestProcessor} from where a worker runnable will process them in bulks.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchWorkProcessor implements AutoCloseable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	/**
	 * Maximum number of requests sent in a single bulk. Could be made an option if needed.
	 */
	private static final int MAX_BULK_SIZE = 250;

	private final AsyncBackendRequestProcessor asyncProcessor;
	private final ErrorHandler errorHandler;
	private final ElasticsearchClient client;
	private final GsonProvider gsonProvider;
	private final ElasticsearchWorkFactory workFactory;
	private final ElasticsearchWorkExecutionContext parallelWorkExecutionContext;

	public ElasticsearchWorkProcessor(BuildContext context,
			ElasticsearchClient client, GsonProvider gsonProvider, ElasticsearchWorkFactory workFactory) {
		asyncProcessor = new AsyncBackendRequestProcessor();
		this.errorHandler = context.getErrorHandler();
		this.client = client;
		this.gsonProvider = gsonProvider;
		this.workFactory = workFactory;
		this.parallelWorkExecutionContext =
				new ParallelWorkExecutionContext( client, gsonProvider );
	}

	@Override
	public void close() {
		awaitAsyncProcessingCompletion();
		asyncProcessor.shutdown();
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
		SequentialWorkExecutionContext context = new SequentialWorkExecutionContext(
				client, gsonProvider, workFactory, this, errorHandler );
		doExecuteSyncSafe( context, works, true );
		context.flush();
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
		asyncProcessor.submit( Collections.singleton( work ) );
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
		asyncProcessor.submit( works );
	}

	/**
	 * Blocks until the queue of requests scheduled for asynchronous processing has been fully processed.
	 * N.B. if more work is added to the queue in the meantime, this might delay the wait.
	 */
	public void awaitAsyncProcessingCompletion() {
		asyncProcessor.awaitCompletion();
	}

	/*
	 * Execute a list of works, bulking them as necessary, and passing any exception to the error handler.
	 *
	 * After an exception, the remaining works in the list are not executed,
	 * though some may have already been executed if they were bulked with the failing work.
	 */
	private void doExecuteSyncSafe(SequentialWorkExecutionContext context,
			Iterable<ElasticsearchWork<?>> nonBulkedWorks, boolean refreshInBulkAPICall) {
		ErrorContextBuilder errorContextBuilder = new ErrorContextBuilder();

		CompletableFuture<?> workListFuture = CompletableFuture.completedFuture( null );

		for ( ElasticsearchWork<?> work : createRequestGroups( nonBulkedWorks, refreshInBulkAPICall ) ) {
			workListFuture = workListFuture.thenCompose( ignored ->
					start( work, context )
							/*
							 * Note that the handler is applied to the "inner" (to be composed) work,
							 * so that the handler is only executed if *this* work fails,
							 * not if a previous one fails.
							 */
							.handle( Futures.handler(
									(result, throwable) -> {
										handleWorkCompletion( errorContextBuilder, throwable, nonBulkedWorks, work );
										return result;
									}
							) )
			);
		}

		/*
		 * Ignore SequenceAbortedExceptions: if we get such an exception,
		 * it means the cause was correctly reported to the handler.
		 */
		workListFuture.exceptionally( Futures.handler( e -> {
					if ( e instanceof SequenceAbortedException ) {
						return null;
					}
					else {
						throw Throwables.expectRuntimeException( e );
					}
				} ) )
				// Note: timeout is handled by the client, so this "join" will not last forever
				.join();
	}

	private <T> CompletableFuture<T> start(ElasticsearchWork<T> work, ElasticsearchWorkExecutionContext context) {
		LOG.tracef( "Processing %s", work );
		return work.execute( context );
	}

	private void handleWorkCompletion(ErrorContextBuilder errorContextBuilder, Throwable throwable,
			Iterable<ElasticsearchWork<?>> nonBulkedWorks, ElasticsearchWork<?> workThatFailed) {
		if ( throwable instanceof BulkRequestFailedException ) {
			BulkRequestFailedException brfe = (BulkRequestFailedException) throwable;
			brfe.getSuccessfulItems().keySet().stream()
					.flatMap( ElasticsearchWork::getLuceneWorks )
					.forEach( errorContextBuilder::workCompleted );

			handleError(
					errorContextBuilder,
					brfe,
					nonBulkedWorks,
					brfe.getErroneousItems().stream()
							.flatMap( ElasticsearchWork::getLuceneWorks )
					);
			/*
			 * Note that we re-throw the throwable,
			 * so that the following works are not executed if this work failed.
			 */
			throw new SequenceAbortedException( throwable );
		}
		else if ( throwable != null ) {
			handleError(
					errorContextBuilder,
					throwable,
					nonBulkedWorks,
					workThatFailed.getLuceneWorks()
					);
			// Same as above
			throw new SequenceAbortedException( throwable );
		}
		else {
			workThatFailed.getLuceneWorks().forEach( errorContextBuilder::workCompleted );
		}
	}

	private void handleError(ErrorContextBuilder errorContextBuilder, Throwable throwable,
			Iterable<ElasticsearchWork<?>> allWorks, Stream<LuceneWork> worksThatFailed) {
		errorContextBuilder.allWorkToBeDone(
				StreamSupport.stream( allWorks.spliterator(), false )
						.flatMap( w -> w.getLuceneWorks() )
						.collect( Collectors.toList() )
				);

		worksThatFailed.forEach( errorContextBuilder::addWorkThatFailed );

		errorContextBuilder.errorThatOccurred( throwable );

		errorHandler.handle( errorContextBuilder.createErrorContext() );
	}

	/**
	 * Organizes the given work list into {@link ProcessorWork}s to be executed.
	 */
	private List<ElasticsearchWork<?>> createRequestGroups(Iterable<ElasticsearchWork<?>> requests, boolean refreshInBulkAPICall) {
		ProcessorWorkGroupBuilder bulkBuilder = new ProcessorWorkGroupBuilder( refreshInBulkAPICall );

		for ( ElasticsearchWork<?> request : requests ) {
			request.aggregate( bulkBuilder );
		}

		return bulkBuilder.build();
	}

	/**
	 * Processes requests asynchronously.
	 * <p>
	 * Incoming messages are submitted to a queue. A worker runnable takes all messages in the queue at a given time and
	 * processes them as a bulk as far as possible. The worker is started upon first message arrival after the queue has
	 * been emptied and remains active until the queue is empty again.
	 *
	 * @author Gunnar Morling
	 */
	private class AsyncBackendRequestProcessor {

		private final ScheduledExecutorService scheduler;
		private final MultiWriteDrainableLinkedList<Iterable<ElasticsearchWork<?>>> asyncWorkQueue;
		private final AtomicBoolean asyncWorkerWasStarted;

		private volatile CountDownLatch lastAsyncWorkLatch;

		private AsyncBackendRequestProcessor() {
			asyncWorkQueue = new MultiWriteDrainableLinkedList<>();
			scheduler = Executors.newScheduledThreadPool( "Elasticsearch AsyncBackendRequestProcessor" );
			asyncWorkerWasStarted = new AtomicBoolean( false );
		}

		public void submit(Iterable<ElasticsearchWork<?>> works) {
			asyncWorkQueue.add( works );
			ensureStarted();
		}

		private void ensureStarted() {
			// Set up worker if needed
			if ( !asyncWorkerWasStarted.get() ) {
				synchronized ( AsyncBackendRequestProcessor.this ) {
					if ( asyncWorkerWasStarted.compareAndSet( false, true ) ) {
						try {
							RequestProcessingRunnable runnable = new RequestProcessingRunnable( this );
							scheduler.schedule( runnable, 100, TimeUnit.MILLISECONDS );
							//only assign this when the job was successfully scheduled:
							lastAsyncWorkLatch = runnable.latch;
						}
						catch (Exception e) {
							// Make sure a failure to setup the worker doesn't leave other threads waiting indefinitely:
							asyncWorkerWasStarted.set( false );
							final CountDownLatch latch = lastAsyncWorkLatch;
							if ( latch != null ) {
								latch.countDown();
							}
							throw e;
						}
					}
				}
			}
		}

		public void awaitCompletion() {
			final CountDownLatch localLatch = lastAsyncWorkLatch;
			if ( localLatch != null ) {
				try {
					localLatch.await();
				}
				catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					throw LOG.interruptedWhileWaitingForRequestCompletion( e );
				}
			}
		}

		public void shutdown() {
			scheduler.shutdown();
			try {
				scheduler.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS );
			}
			catch (InterruptedException e) {
				LOG.interruptedWhileWaitingForIndexActivity( e );
			}
			finally {
				final CountDownLatch localLatch = lastAsyncWorkLatch;
				if ( localLatch != null ) {
					//It's possible that a task was successfully scheduled but had no chance to run,
					//so we need to release waiting threads:
					localLatch.countDown();
				}
			}
		}
	}

	/**
	 * Takes requests from the queue and processes them.
	 */
	private class RequestProcessingRunnable implements Runnable {

		private final AsyncBackendRequestProcessor asyncProcessor;
		private final CountDownLatch latch = new CountDownLatch( 1 );

		public RequestProcessingRunnable(AsyncBackendRequestProcessor asyncProcessor) {
			this.asyncProcessor = asyncProcessor;
		}

		@Override
		public void run() {
			try {
				processAsyncWork();
			}
			finally {
				latch.countDown();
			}
		}

		private void processAsyncWork() {
			SequentialWorkExecutionContext context = new SequentialWorkExecutionContext(
					client, gsonProvider, workFactory, ElasticsearchWorkProcessor.this, errorHandler );
			synchronized ( asyncProcessor ) {
				while ( true ) {
					Iterable<Iterable<ElasticsearchWork<?>>> works = asyncProcessor.asyncWorkQueue.drainToDetachedIterable();
					if ( works == null ) {
						// Allow other async processors to be setup already as we're on our way to termination:
						asyncProcessor.asyncWorkerWasStarted.set( false );
						// Nothing more to do, flush and terminate:
						context.flush();
						return;
					}
					Iterable<ElasticsearchWork<?>> flattenedWorks = CollectionHelper.flatten( works );
					doExecuteSyncSafe( context, flattenedWorks, false );
				}
			}
		}
	}

	private class ProcessorWorkGroupBuilder implements ElasticsearchWorkAggregator {

		private final boolean refreshInBulkAPICall;

		private final List<ElasticsearchWork<?>> result = new ArrayList<>();
		private final List<BulkableElasticsearchWork<?>> bulkInProgress = new ArrayList<>();

		public ProcessorWorkGroupBuilder(boolean refreshInBulkAPICall) {
			super();
			this.refreshInBulkAPICall = refreshInBulkAPICall;
		}

		@Override
		public void addBulkable(BulkableElasticsearchWork<?> work) {
			bulkInProgress.add( work );
			if ( bulkInProgress.size() >= MAX_BULK_SIZE ) {
				flushBulkInProgress();
			}
		}

		@Override
		public void addNonBulkable(ElasticsearchWork<?> work) {
			flushBulkInProgress();
			result.add( work );
		}

		private void flushBulkInProgress() {
			if ( bulkInProgress.isEmpty() ) {
				return;
			}

			if ( bulkInProgress.size() == 1 ) {
				ElasticsearchWork<?> work = bulkInProgress.iterator().next();
				result.add( work );
			}
			else {
				result.add( workFactory.bulk( bulkInProgress ).refresh( refreshInBulkAPICall ).build() );
			}
			bulkInProgress.clear();
		}

		private List<ElasticsearchWork<?>> build() {
			flushBulkInProgress();
			return result;
		}
	}
}
