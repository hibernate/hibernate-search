/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.backend.elasticsearch.impl.BackendRequest;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

import io.searchbox.indices.Refresh;

/**
 * A request group backed by an actual bulk request.
 */
public class BackendRequestBulk implements BackendRequestGroup {

	private final JestClient jestClient;
	private final ErrorHandler errorHandler;
	private final List<BackendRequest<?>> requests;
	private final boolean refresh;
	private final Set<String> indexNames;

	public BackendRequestBulk(JestClient jestClient, ErrorHandler errorHandler, List<BackendRequest<?>> requests, Set<String> indexNames, boolean refresh) {
		this.jestClient = jestClient;
		this.errorHandler = errorHandler;
		this.requests = requests;
		this.indexNames = indexNames;
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
			Refresh.Builder refreshBuilder = new Refresh.Builder();

			for ( String index : indexNames ) {
				refreshBuilder.addIndex( index );
			}

			jestClient.executeRequest( refreshBuilder.build() );
		}
	}

	@Override
	public String toString() {
		return "BulkRequest [size=" + requests.size() + ", refresh=" + refresh + ", indexNames=" + indexNames + "]";
	}
}
