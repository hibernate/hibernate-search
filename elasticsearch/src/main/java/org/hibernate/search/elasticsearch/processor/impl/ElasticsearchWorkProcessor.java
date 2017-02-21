/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.impl.lucene.MultiWriteDrainableLinkedList;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchService;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialect;
import org.hibernate.search.elasticsearch.dialect.impl.ElasticsearchDialectProvider;
import org.hibernate.search.elasticsearch.impl.GsonService;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.elasticsearch.work.impl.BulkRequestFailedException;
import org.hibernate.search.elasticsearch.work.impl.BulkableElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWork;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;
import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkExecutionContext;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.ServiceManager;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.impl.Executors;
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
public class ElasticsearchWorkProcessor implements Service, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	/**
	 * Maximum number of requests sent in a single bulk. Could be made an option if needed.
	 */
	private static final int MAX_BULK_SIZE = 250;

	private final AsyncBackendRequestProcessor asyncProcessor;
	private ErrorHandler errorHandler;
	private ServiceManager serviceManager;
	private ElasticsearchService elasticsearchService;
	private GsonService gsonService;
	private ElasticsearchWorkExecutionContext parallelWorkExecutionContext;
	private ElasticsearchDialect dialect;

	public ElasticsearchWorkProcessor() {
		asyncProcessor = new AsyncBackendRequestProcessor();
	}

	@Override
	public void start(Properties properties, BuildContext context) {
		this.errorHandler = context.getErrorHandler();
		this.serviceManager = context.getServiceManager();
		this.elasticsearchService = serviceManager.requestService( ElasticsearchService.class );
		this.gsonService = serviceManager.requestService( GsonService.class );
		this.parallelWorkExecutionContext =
				new ParallelWorkExecutionContext( elasticsearchService.getClient(), gsonService );
		ElasticsearchDialectProvider dialectProvider = serviceManager.requestService( ElasticsearchDialectProvider.class );
		this.dialect = dialectProvider.getDialect();
	}

	@Override
	public void stop() {
		awaitAsyncProcessingCompletion();
		asyncProcessor.shutdown();

		dialect = null;
		serviceManager.releaseService( ElasticsearchDialectProvider.class );
		gsonService = null;
		serviceManager.releaseService( GsonService.class );
		elasticsearchService = null;
		serviceManager.releaseService( ElasticsearchService.class );
		serviceManager = null;
	}

	/**
	 * Executes a work synchronously, potentially throwing exceptions (the error handler isn't used).
	 */
	public <T> T executeSyncUnsafe(ElasticsearchWork<T> work) {
		return work.execute( parallelWorkExecutionContext );
	}

	/**
	 * Executes works synchronously, passing any thrown exception to the error handler.
	 */
	public void executeSyncSafe(Iterable<ElasticsearchWork<?>> requests) {
		executeSafely( requests );
	}

	/**
	 * Executes a work asynchronously, passing any exception to the error handler.
	 */
	public void executeAsync(ElasticsearchWork<?> request) {
		asyncProcessor.submitRequest( request );
	}

	/**
	 * Blocks until the queue of requests scheduled for asynchronous processing has been fully processed.
	 * N.B. if more work is added to the queue in the meantime, this might delay the wait.
	 */
	public void awaitAsyncProcessingCompletion() {
		asyncProcessor.awaitCompletion();
	}

	/**
	 * Groups the given work list into executable bulks and executes them. For each bulk, the error handler - if
	 * registered - will be invoked with the items of that bulk.
	 */
	private void executeSafely(Iterable<ElasticsearchWork<?>> requests) {
		SequentialWorkExecutionContext context = new SequentialWorkExecutionContext(
				elasticsearchService.getClient(),
				dialect, this, gsonService, errorHandler );

		for ( ElasticsearchWork<?> work : createRequestGroups( requests, true ) ) {
			executeSafely( work, context );
		}

		context.flush();
	}

	private void executeSafely(ElasticsearchWork<?> work, ElasticsearchWorkExecutionContext context) {
		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Processing %s", work );
		}

		try {
			work.execute( context );
		}
		catch (BulkRequestFailedException brfe) {
			ErrorContextBuilder builder = new ErrorContextBuilder();
			List<LuceneWork> allWorks = new ArrayList<>();

			for ( BulkableElasticsearchWork<?> successfulWork : brfe.getSuccessfulItems().keySet() ) {
				successfulWork.getLuceneWorks().forEach( (w) -> {
						allWorks.add( w );
						builder.workCompleted( w );
				});
			}

			for ( BulkableElasticsearchWork<?> failedWork : brfe.getErroneousItems() ) {
				failedWork.getLuceneWorks().forEach( (w) -> {
						allWorks.add( w );
						builder.addWorkThatFailed( w );
				});
			}

			builder.allWorkToBeDone( allWorks );

			builder.errorThatOccurred( brfe );

			errorHandler.handle( builder.createErrorContext() );
		}
		catch (RuntimeException e) {
			ErrorContextBuilder builder = new ErrorContextBuilder();
			List<LuceneWork> allWorks = new ArrayList<>();

			work.getLuceneWorks().forEach( (w) -> {
					allWorks.add( w );
					builder.addWorkThatFailed( w );
			});

			builder.allWorkToBeDone( allWorks );

			builder.errorThatOccurred( e );

			errorHandler.handle( builder.createErrorContext() );
		}
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
		private final MultiWriteDrainableLinkedList<ElasticsearchWork<?>> asyncWorkQueue;
		private final AtomicBoolean asyncWorkerWasStarted;

		private volatile CountDownLatch lastAsyncWorkLatch;

		private AsyncBackendRequestProcessor() {
			asyncWorkQueue = new MultiWriteDrainableLinkedList<>();
			scheduler = Executors.newScheduledThreadPool( "Elasticsearch AsyncBackendRequestProcessor" );
			asyncWorkerWasStarted = new AtomicBoolean( false );
		}

		public void submitRequest(ElasticsearchWork<?> request) {
			asyncWorkQueue.add( request );

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
					elasticsearchService.getClient(),
					dialect, ElasticsearchWorkProcessor.this, gsonService, errorHandler );
			synchronized ( asyncProcessor ) {
				while ( true ) {
					Iterable<ElasticsearchWork<?>> works = asyncProcessor.asyncWorkQueue.drainToDetachedIterable();
					if ( works == null ) {
						// Allow other async processors to be setup already as we're on our way to termination:
						asyncProcessor.asyncWorkerWasStarted.set( false );
						// Nothing more to do, flush and terminate:
						context.flush();
						return;
					}
					for ( ElasticsearchWork<?> work : createRequestGroups( works, false ) ) {
						work.execute( context );
					}
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
				result.add( dialect.getWorkFactory().bulk( bulkInProgress ).refresh( refreshInBulkAPICall ).build() );
			}
			bulkInProgress.clear();
		}

		private List<ElasticsearchWork<?>> build() {
			flushBulkInProgress();
			return result;
		}
	}
}
