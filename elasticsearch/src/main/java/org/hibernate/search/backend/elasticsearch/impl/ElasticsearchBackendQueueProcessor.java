/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.locks.Lock;

import org.hibernate.search.backend.IndexingMonitor;
import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.elasticsearch.client.impl.BulkRequestFailedException;
import org.hibernate.search.backend.elasticsearch.client.impl.JestClient;
import org.hibernate.search.backend.spi.BackendQueueProcessor;
import org.hibernate.search.engine.integration.impl.ExtendedSearchIntegrator;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;
import org.hibernate.search.indexes.spi.IndexManager;
import org.hibernate.search.spi.WorkerBuildContext;

import io.searchbox.action.BulkableAction;
import io.searchbox.core.DeleteByQuery;
import io.searchbox.indices.Refresh;

/**
 * {@link BackendQueueProcessor} applying index changes to an Elasticsearch server.
 *
 * @author Gunnar Morling
 */
public class ElasticsearchBackendQueueProcessor implements BackendQueueProcessor {

	private ElasticsearchIndexManager indexManager;
	private ExtendedSearchIntegrator searchIntegrator;
	private ElasticsearchIndexWorkVisitor visitor;
	private ErrorHandler errorHandler;
	private JestClient jestClient;

	@Override
	public void initialize(Properties props, WorkerBuildContext context, IndexManager indexManager) {
		this.indexManager = (ElasticsearchIndexManager) indexManager;
		this.errorHandler = context.getErrorHandler();
		this.searchIntegrator = context.getUninitializedSearchIntegrator();
		this.visitor = new ElasticsearchIndexWorkVisitor(
				this.indexManager.getActualIndexName(),
				this.searchIntegrator
		);
		this.jestClient = context.getServiceManager().requestService( JestClient.class );
	}

	@Override
	public void close() {
		searchIntegrator.getServiceManager().releaseService( JestClient.class );
	}

	@Override
	public void applyWork(List<LuceneWork> workList, IndexingMonitor monitor) {
		// Run single action, with refresh
		if ( workList.size() == 1 ) {
			doApplySingleWork( workList.iterator().next() );
		}
		// Create bulk action
		else {
			doApplyListOfWork( workList );
		}
	}

	/**
	 * Groups the given work list into executable bulks and executes them. For each bulk, the error handler - if
	 * registered - will be invoked with the items of that bulk.
	 */
	private void doApplyListOfWork(List<LuceneWork> workList) {
		BackendRequestGroup nextBulk = null;

		for ( BackendRequestGroup backendRequestGroup : createRequestGroups( workList ) ) {
			nextBulk = backendRequestGroup;
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
	 * Organizes the given work list into {@link BackendRequestGroup}s to be executed.
	 */
	private List<BackendRequestGroup> createRequestGroups(List<LuceneWork> workList) {
		List<BackendRequestGroup> groups = new ArrayList<>();
		List<BackendRequest<?>> currentBulk = new ArrayList<>();

		for ( LuceneWork luceneWork : workList ) {
			BackendRequest<?> request = luceneWork.acceptIndexWorkVisitor( visitor, false );

			// either add to current bulk...
			if ( request.getAction() instanceof BulkableAction ) {
				currentBulk.add( request );
			}
			// ... or finish up current bulk and add single request for non-bulkable request
			else {
				if ( !currentBulk.isEmpty() ) {
					groups.add( new BackendRequestBulk( currentBulk, false ) );
					currentBulk.clear();
				}
				groups.add( new SingleRequest( request ) );
			}
		}

		// finish up last bulk
		if ( !currentBulk.isEmpty() ) {
			groups.add( new BackendRequestBulk( currentBulk, true ) );
		}

		return groups;
	}

	private void refreshIndex() {
		Refresh refresh = new Refresh.Builder().addIndex( indexManager.getActualIndexName() ).build();
		jestClient.executeRequest( refresh );
	}

	@Override
	public void applyStreamWork(LuceneWork singleOperation, IndexingMonitor monitor) {
		doApplySingleWork( singleOperation );
	}

	private void doApplySingleWork(LuceneWork work) {
		try {
			BackendRequest<?> request = work.acceptIndexWorkVisitor( visitor, true );

			if ( request == null ) {
				return;
			}

			jestClient.executeRequest( request.getAction(), request.getIgnoredErrorStatuses() );

			// DBQ ignores the refresh parameter for some reason, so doing it explicitly
			if ( request.getAction() instanceof DeleteByQuery ) {
				refreshIndex();
			}
		}
		catch (Exception e) {
			ErrorContextBuilder builder = new ErrorContextBuilder();

			builder.allWorkToBeDone( Collections.singleton( work ) );
			builder.addWorkThatFailed( work );
			builder.errorThatOccurred( e );

			errorHandler.handle( builder.createErrorContext() );
		}
	}

	@Override
	public Lock getExclusiveWriteLock() {
		// TODO implement
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void indexMappingChanged() {
		// TODO implement
		throw new UnsupportedOperationException( "Not implemented yet" );
	}

	@Override
	public void closeIndexWriter() {
		// no-op
	}

	/**
	 * Represents a group of backend requests, which may either be backed by an actual bulk request or by a single
	 * request "pseudo group". Allows for uniform handling of these two cases.
	 */
	private interface BackendRequestGroup {
		void execute();
		void ensureRefreshed();
	}

	/**
	 * A request group backed by an actual bulk request.
	 */
	private class BackendRequestBulk implements BackendRequestGroup {

		private final List<BackendRequest<?>> requests;
		private final boolean refresh;

		public BackendRequestBulk(List<BackendRequest<?>> requests, boolean refresh) {
			this.requests = requests;
			this.refresh = refresh;
		}

		@Override
		public void execute() {
			try {
				jestClient.executeBulkRequest( requests, refresh );
			}
			catch (BulkRequestFailedException brfe) {
				ErrorContextBuilder builder = new ErrorContextBuilder();
				List<LuceneWork> allWork = new ArrayList<>();

				for ( BackendRequest<?> backendRequest : requests ) {
					allWork.add( backendRequest.getLuceneWork() );
					if ( !brfe.getErroneousItems().contains( backendRequest ) ) {
						builder.workCompleted( backendRequest.getLuceneWork() );
					}
				}

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

		@Override
		public void ensureRefreshed() {
			if ( !refresh ) {
				refreshIndex();
			}
		}
	}

	/**
	 * A "group" just comprising a single, non-bulkable request.
	 */
	private class SingleRequest implements BackendRequestGroup {

		private final BackendRequest<?> request;

		public SingleRequest(BackendRequest<?> request) {
			this.request = request;
		}

		@Override
		public void execute() {
			try {
				jestClient.executeRequest( request.getAction(), request.getIgnoredErrorStatuses() );
			}
			catch (Exception e) {
				ErrorContextBuilder builder = new ErrorContextBuilder();

				builder.allWorkToBeDone( Collections.singletonList( request.getLuceneWork() ) );
				builder.addWorkThatFailed( request.getLuceneWork() );
				builder.errorThatOccurred( e );

				errorHandler.handle( builder.createErrorContext() );
			}
		}

		@Override
		public void ensureRefreshed() {
			refreshIndex();
		}
	}
}
