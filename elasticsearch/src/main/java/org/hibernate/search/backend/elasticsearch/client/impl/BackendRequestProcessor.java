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
import io.searchbox.indices.Refresh;

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

	/**
	 * Maximum number of requests sent in a single bulk. Could be made an option if needed.
	 */
	private static final int MAX_BULK_SIZE = 250;

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
		Set<String> indexesNeedingRefresh = new HashSet<>();

		for ( ExecutableRequest backendRequestGroup : createRequestGroups( requests ) ) {
			nextBulk = backendRequestGroup;

			if ( LOG.isTraceEnabled() ) {
				LOG.tracef( "Processing bulk of %s items on index(es) %s", nextBulk.getSize(), nextBulk.getTouchedIndexes() );
			}

			nextBulk.execute();
			indexesNeedingRefresh.addAll( backendRequestGroup.getTouchedIndexes() );
			indexesNeedingRefresh.removeAll( backendRequestGroup.getRefreshedIndexes() );
		}

		refresh( indexesNeedingRefresh );
	}

	/**
	 * Organizes the given work list into {@link ExecutableRequest}s to be executed.
	 */
	private List<ExecutableRequest> createRequestGroups(Iterable<BackendRequest<?>> requests) {
		List<ExecutableRequest> groups = new ArrayList<>();
		BulkRequestBuilder bulkBuilder = new BulkRequestBuilder();

		for ( BackendRequest<?> request : requests ) {
			boolean currentRequestBulkable = request.getAction() instanceof BulkableAction;
			boolean currentBulkNeedsFinishing = ( !bulkBuilder.canAddMore() || !currentRequestBulkable ) && !bulkBuilder.isEmpty();

			// finish up current bulk
			if ( currentBulkNeedsFinishing ) {
				groups.add( bulkBuilder.build( false ) );
				bulkBuilder = new BulkRequestBuilder();
			}

			// either add to current bulk...
			if ( currentRequestBulkable ) {
				bulkBuilder.add( request );
			}
			// ...  or add single request for non-bulkable request
			else {
				groups.add( new SingleRequest( jestClient, errorHandler, request ) );
			}
		}

		// finish up last bulk
		if ( !bulkBuilder.isEmpty() ) {
			groups.add( bulkBuilder.build( true ) );
		}

		return groups;
	}

	/**
	 * Performs an explicit refresh of the given index(es).
	 */
	private void refresh(Set<String> indexesNeedingRefresh) {
		if ( indexesNeedingRefresh.isEmpty() ) {
			return;
		}

		if ( LOG.isTraceEnabled() ) {
			LOG.tracef( "Refreshing index(es) %s", indexesNeedingRefresh );
		}

		Refresh.Builder refreshBuilder = new Refresh.Builder();

		for ( String index : indexesNeedingRefresh ) {
			refreshBuilder.addIndex( index );
		}

		try {
			jestClient.executeRequest( refreshBuilder.build() );
		}
		catch (BulkRequestFailedException brfe) {
			errorHandler.handleException( "Refresh failed", brfe );
		}
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

	private class BulkRequestBuilder {

		private final List<BackendRequest<?>> bulk = new ArrayList<>();
		private final Set<String> indexNames = new HashSet<>();
		private int size = 0;

		private void add(BackendRequest<?> request) {
			bulk.add( request );
			indexNames.add( request.getIndexName() );
			size++;
		}
		private boolean canAddMore() {
			return size < MAX_BULK_SIZE;
		}

		private boolean isEmpty() {
			return size == 0;
		}

		private ExecutableRequest build(boolean refresh) {
			if ( size > 1 ) {
				return new BulkRequest( jestClient, errorHandler, bulk, indexNames, refresh );
			}
			else {
				return new SingleRequest( jestClient, errorHandler, bulk.iterator().next() );
			}
		}
	}
}
