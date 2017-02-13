/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.elasticsearch.client.impl.BackendRequest;
import org.hibernate.search.elasticsearch.client.impl.JestClient;
import org.hibernate.search.elasticsearch.impl.JestAPIFormatter;
import org.hibernate.search.elasticsearch.impl.NoOpBackendRequestResultAssessor;
import org.hibernate.search.elasticsearch.logging.impl.Log;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.util.impl.CollectionHelper;
import org.hibernate.search.util.logging.impl.LoggerFactory;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.Bulk;
import io.searchbox.core.BulkResult;
import io.searchbox.core.Bulk.Builder;
import io.searchbox.core.BulkResult.BulkResultItem;
import io.searchbox.params.Parameters;

/**
 * A request group backed by an actual bulk request.
 */
class BulkRequest implements ExecutableRequest {

	private static final Log LOG = LoggerFactory.make( Log.class );

	private final JestClient jestClient;
	private final JestAPIFormatter jestAPIFormatter;
	private final ErrorHandler errorHandler;
	private final List<BackendRequest<?>> requests;

	/**
	 * Whether to perform a refresh in the course of executing this bulk or not. Note that this will refresh all indexes
	 * touched by this bulk, not only those given via {@link #indexesNeedingRefresh}. That's acceptable.
	 * <p>
	 * If {@code true}, no explicit refresh of the concerned indexes is needed afterwards.
	 */
	private final boolean refresh;
	private final Set<String> indexNames;

	/**
	 * Names of those indexes to be refreshed after executing this bulk
	 */
	private final Set<String> indexesNeedingRefresh;

	public BulkRequest(JestClient jestClient, JestAPIFormatter jestAPIFormatter,
			ErrorHandler errorHandler, List<BackendRequest<?>> requests,
			Set<String> indexNames, Set<String> indexesNeedingRefresh, boolean refresh) {
		this.jestClient = jestClient;
		this.jestAPIFormatter = jestAPIFormatter;
		this.errorHandler = errorHandler;
		this.requests = requests;
		this.indexNames = indexNames;
		this.indexesNeedingRefresh = indexesNeedingRefresh;
		this.refresh = refresh;
	}

	@Override
	public void execute() {
		Map<BackendRequest<?>, BulkResultItem> results = null;
		try {
			results = doExecute();
			RuntimeException e = reportResults( results, null );
			if ( e != null ) {
				throw e; // Handle the exception below
			}
		}
		catch (BulkRequestFailedException brfe) {
			// Call the result handler anyway for those requests that succeeded
			reportResults( brfe.getSuccessfulItems(), brfe );

			ErrorContextBuilder builder = new ErrorContextBuilder();
			List<LuceneWork> allWork = new ArrayList<>();

			builder.allWorkToBeDone( allWork );

			for ( BackendRequest<?> failedAction : brfe.getErroneousItems() ) {
				builder.addWorkThatFailed( failedAction.getLuceneWork() );
			}

			builder.errorThatOccurred( brfe );

			errorHandler.handle( builder.createErrorContext() );
		}
		catch (Exception e) {
			errorHandler.handleException( "Bulk request failed", e );
		}
	}

	private Map<BackendRequest<?>, BulkResultItem> doExecute() {
		Builder bulkBuilder = new Bulk.Builder()
				.setParameter( Parameters.REFRESH, refresh );

		for ( BackendRequest<?> backendRequest : requests ) {
			bulkBuilder.addAction( (BulkableAction<?>) backendRequest.getAction() );
		}

		Bulk request = bulkBuilder.build();

		BulkResult response = jestClient.executeRequest( request, NoOpBackendRequestResultAssessor.INSTANCE );

		Map<BackendRequest<?>, BulkResultItem> successfulItems =
				CollectionHelper.newHashMap( requests.size() );

		/*
		 * We can't rely on the status of the bulk, since each backend request may consider specific
		 * status codes as a success regardless of their usual meaning, which Elasticsearch doesn't
		 * know about when computing the status of the bulk.
		 */
		List<BackendRequest<?>> erroneousItems = new ArrayList<>();
		int i = 0;
		for ( BulkResultItem resultItem : response.getItems() ) {
			BackendRequest<?> backendRequest = requests.get( i );

			if ( backendRequest.getResultAssessor().isSuccess( resultItem ) ) {
				successfulItems.put( backendRequest, resultItem );
			}
			else {
				erroneousItems.add( backendRequest );
			}
			++i;
		}

		if ( !erroneousItems.isEmpty() ) {
			throw LOG.elasticsearchBulkRequestFailed(
					jestAPIFormatter.formatRequest( request ),
					jestAPIFormatter.formatResult( response ),
					successfulItems,
					erroneousItems
			);
		}
		else {
			return successfulItems;
		}
	}

	/**
	 * Call every result reporter making sure that:<ul>
	 * <li>calls to indexing monitors are grouped, meaning if 5 results require a call to {@code documentsAdded}
	 * (for instance), only one call will be done with {@code 5L} as a parameter.
	 * <li>exceptions are handled properly so that one failing reporter will not prevent others from being called
	 * </ul>
	 *
	 * <p>No locally caught exception will be re-thrown, but the "main" exception will be returned.
	 * The "main" exception is either the {@code preexistingMainException} parameter (if non-null) or the first
	 * locally caught exception. Every locally caught exception that is not the "main" exception will be
	 * added as suppressed to the "main" exception.
	 *
	 * @param successfulItems The requests and their results, for calling result reporters.
	 * @param preexistingMainException The exception to use as a "main" exception (see above), or {@code null}.
	 * @return The "main" exception (see above), or {@code null} if there isn't any.
	 */
	private RuntimeException reportResults(Map<BackendRequest<?>, BulkResultItem> successfulItems, RuntimeException preexistingMainException) {
		/*
		 * We use buffers to avoid too many calls to the actual index monitor, which is potentially synchronized
		 * and hence may be a contention point.
		 */
		Map<IndexingMonitor, BufferingIndexMonitor> buffers = new HashMap<>();

		RuntimeException mainException = preexistingMainException;
		for ( Map.Entry<BackendRequest<?>, BulkResultItem> entry : successfulItems.entrySet() ) {
			try {
				reportResult( buffers, entry );
			}
			catch (RuntimeException e) {
				mainException = returnOrSuppressException( mainException, e );
			}
		}

		// Flush the buffers
		for ( BufferingIndexMonitor buffer : buffers.values() ) {
			try {
				buffer.flush();
			}
			catch (RuntimeException e) {
				mainException = returnOrSuppressException( mainException, e );
			}
		}

		return mainException;
	}

	private RuntimeException returnOrSuppressException(RuntimeException existingException, RuntimeException newException) {
		// Do not stop calling handlers just because *one* handler failed, report the error later
		if ( existingException == null ) {
			return newException;
		}
		else {
			existingException.addSuppressed( newException );
			return existingException;
		}
	}

	private void reportResult(Map<IndexingMonitor, BufferingIndexMonitor> buffers, Entry<BackendRequest<?>, BulkResultItem> entry) {
		BackendRequest<?> request = entry.getKey();
		BulkResultItem result = entry.getValue();

		IndexingMonitor originalMonitor = request.getIndexingMonitor();
		if ( originalMonitor == null ) {
			return;
		}

		BufferingIndexMonitor bufferingMonitor = buffers.get( originalMonitor );
		if ( bufferingMonitor == null ) {
			bufferingMonitor = new BufferingIndexMonitor( originalMonitor );
			buffers.put( originalMonitor, bufferingMonitor );
		}

		request.getSuccessReporter().report( result, bufferingMonitor );
	}

	@Override
	public Set<String> getTouchedIndexes() {
		return indexNames;
	}

	@Override
	public Set<String> getIndexesNeedingRefresh() {
		if ( refresh ) {
			return Collections.emptySet();
		}
		else {
			return indexesNeedingRefresh;
		}
	}

	@Override
	public int getSize() {
		return requests.size();
	}

	@Override
	public String toString() {
		return "BulkRequest [size=" + requests.size() + ", refresh=" + refresh + ", indexNames=" + indexNames + ", indexesNeedingRefresh=" + indexesNeedingRefresh
				+ "]";
	}

	private static final class BufferingIndexMonitor implements IndexingMonitor {

		private final IndexingMonitor delegate;

		private long documentsAdded = 0L;

		public BufferingIndexMonitor(IndexingMonitor delegate) {
			super();
			this.delegate = delegate;
		}

		@Override
		public void documentsAdded(long increment) {
			documentsAdded += increment;
		}

		private void flush() {
			delegate.documentsAdded( documentsAdded );
			documentsAdded = 0L;
		}
	}
}
