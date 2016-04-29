/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.hibernate.search.backend.elasticsearch.logging.impl.Log;
import org.hibernate.search.backend.impl.lucene.MultiWriteDrainableLinkedList;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.engine.service.spi.Stoppable;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.SearchException;
import org.hibernate.search.spi.BuildContext;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.BulkableAction;

/**
 * Executes single or multiple {@link BackendRequest}s against the Elasticsearch server. When processing multiple
 * requests, bulk requests will be formed and executed as far as possible.
 * <p>
 * Requests can be processed synchronously or asynchronously. In the latter case, incoming requests are added to a queue
 * via {@link AsyncBackendRequestProcessor} from where a worker runnable will process them in bulks.
 *
 * @author Gunnar Morling
 */
public class BackendRequestProcessor implements Service, Startable, Stoppable {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final AsyncBackendRequestProcessor asyncProcessor;
	private ErrorHandler errorHandler;
	private JestClient jestClient;

	public BackendRequestProcessor() {
		asyncProcessor = new AsyncBackendRequestProcessor();
	}

	@Override
	public void start(Properties properties, BuildContext context) {
		this.errorHandler = context.getErrorHandler();
		this.jestClient = context.getServiceManager().requestService( JestClient.class );
	}

	@Override
	public void stop() {
		awaitAsyncProcessingCompletion();
	}

	public void executeSync(Iterable<BackendRequest<?>> requests) {
		doExecute( requests );
	}

	public void executeSync(BackendRequest<?> request) {
		SingleRequest executableRequest = new SingleRequest( jestClient, errorHandler, request );

		if ( request != null ) {
			executableRequest.execute();
			executableRequest.ensureRefreshed();
		}
	}

	public void executeAsync(BackendRequest<?> request) {
		asyncProcessor.submitRequest( request );
	}

	/**
	 * Blocks until the queue of requests scheduled for asynchronous processing has been fully processed.
	 */
	public void awaitAsyncProcessingCompletion() {
		asyncProcessor.awaitCompletion();
	}

	/**
	 * Groups the given work list into executable bulks and executes them. For each bulk, the error handler - if
	 * registered - will be invoked with the items of that bulk.
	 */
	private void doExecute(Iterable<BackendRequest<?>> requests) {
		ExecutableRequest nextBulk = null;

		for ( ExecutableRequest backendRequestGroup : createRequestGroups( requests ) ) {
			nextBulk = backendRequestGroup;

			if ( LOG.isTraceEnabled() ) {
				LOG.tracef( "Processing bulk of %s items", nextBulk.getSize() );
			}

			nextBulk.execute();
		}

		// Make sure a final refresh has been issued
		try {
			nextBulk.ensureRefreshed();
		}
		catch (BulkRequestFailedException brfe) {
			errorHandler.handleException( "Refresh failed", brfe );
		}
	}

	/**
	 * Organizes the given work list into {@link ExecutableRequest}s to be executed.
	 */
	private List<ExecutableRequest> createRequestGroups(Iterable<BackendRequest<?>> requests) {
		List<ExecutableRequest> groups = new ArrayList<>();
		List<BackendRequest<?>> currentBulk = new ArrayList<>();
		Set<String> currentIndexNames = new HashSet<>();

		for ( BackendRequest<?> request : requests ) {
			// either add to current bulk...
			if ( request.getAction() instanceof BulkableAction ) {
				currentBulk.add( request );
				currentIndexNames.add( request.getIndexName() );
			}
			// ... or finish up current bulk and add single request for non-bulkable request
			else {
				if ( !currentBulk.isEmpty() ) {
					groups.add( new BulkRequest( jestClient, errorHandler, currentBulk, currentIndexNames, false ) );
					currentBulk.clear();
					currentIndexNames.clear();
				}
				groups.add( new SingleRequest( jestClient, errorHandler, request ) );
			}
		}

		// finish up last bulk
		if ( !currentBulk.isEmpty() ) {
			groups.add( new BulkRequest( jestClient, errorHandler, currentBulk, currentIndexNames, true ) );
		}

		return groups;
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
		private final MultiWriteDrainableLinkedList<BackendRequest<?>> asyncRequestQueue;

		private final AtomicBoolean asyncWorkInProcessing = new AtomicBoolean( false );
		private volatile CountDownLatch asyncWorkLatch;

		private AsyncBackendRequestProcessor() {
			asyncRequestQueue = new MultiWriteDrainableLinkedList<>();
			scheduler = Executors.newScheduledThreadPool( 1 );
		}

		public void submitRequest(BackendRequest<?> request) {
			asyncRequestQueue.add( request );

			// Set up worker if needed
			if ( !asyncWorkInProcessing.get() ) {
				synchronized ( this ) {
					if ( !asyncWorkInProcessing.get() ) {
						asyncWorkInProcessing.set( true );
						asyncWorkLatch = new CountDownLatch( 1 );
						scheduler.schedule( new RequestProcessingRunnable( this ), 100, TimeUnit.MILLISECONDS );
					}
				}
			}
		}

		public void awaitCompletion() {
			if ( asyncWorkLatch != null ) {
				try {
					asyncWorkLatch.await();
				}
				catch (InterruptedException e) {
					throw new SearchException( e );
				}
			}
		}
	}

	/**
	 * Takes requests from the queue and processes them.
	 */
	private class RequestProcessingRunnable implements Runnable {

		private final AsyncBackendRequestProcessor asyncProcessor;

		public RequestProcessingRunnable(AsyncBackendRequestProcessor asyncProcessor) {
			this.asyncProcessor = asyncProcessor;
		}

		@Override
		public void run() {
			while ( true ) {
				synchronized ( BackendRequestProcessor.this ) {
					Iterable<BackendRequest<?>> requests = asyncProcessor.asyncRequestQueue.drainToDetachedIterable();

					// Nothing more to do, allow processor to shut down if requested
					if ( requests == null ) {
						asyncProcessor.asyncWorkInProcessing.set( false );
						asyncProcessor.asyncWorkLatch.countDown();
						return;
					}

					doExecute( requests );
				}
			}
		}
	}
}
