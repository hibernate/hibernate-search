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

import org.hibernate.search.backend.elasticsearch.impl.BackendRequest;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.engine.service.spi.Startable;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.spi.BuildContext;

import io.searchbox.action.BulkableAction;

/**
 * Executes single or multiple {@link BackendRequest}s against the Elasticsearch server. When processing multiple
 * requests, bulk requests will be formed and executed as far as possible.
 *
 * @author Gunnar Morling
 */
public class BackendRequestProcessor implements Service, Startable {

	private ErrorHandler errorHandler;
	private JestClient jestClient;

	@Override
	public void start(Properties properties, BuildContext context) {
		this.errorHandler = context.getErrorHandler();
		this.jestClient = context.getServiceManager().requestService( JestClient.class );
	}

	/**
	 * Groups the given work list into executable bulks and executes them. For each bulk, the error handler - if
	 * registered - will be invoked with the items of that bulk.
	 */
	public void executeSync(Iterable<BackendRequest<?>> requests) {
		BackendRequestGroup nextBulk = null;

		for ( BackendRequestGroup backendRequestGroup : createRequestGroups( requests ) ) {
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
	private List<BackendRequestGroup> createRequestGroups(Iterable<BackendRequest<?>> requests) {
		List<BackendRequestGroup> groups = new ArrayList<>();
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
					groups.add( new BackendRequestBulk( jestClient, errorHandler, currentBulk, currentIndexNames, false ) );
					currentBulk.clear();
					currentIndexNames.clear();
				}
				groups.add( new SingleRequest( jestClient, errorHandler, request ) );
			}
		}

		// finish up last bulk
		if ( !currentBulk.isEmpty() ) {
			groups.add( new BackendRequestBulk( jestClient, errorHandler, currentBulk, currentIndexNames, true ) );
		}

		return groups;
	}


	public void executeSync(BackendRequest<?> request) {
		SingleRequest executableRequest = new SingleRequest( jestClient, errorHandler, request );

		if ( request != null ) {
			executableRequest.execute();
			executableRequest.ensureRefreshed();
		}
	}
}
