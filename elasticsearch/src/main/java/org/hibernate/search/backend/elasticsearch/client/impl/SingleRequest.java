/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.impl;

import java.util.Collections;

import org.hibernate.search.backend.elasticsearch.impl.BackendRequest;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

import io.searchbox.core.DeleteByQuery;
import io.searchbox.indices.Refresh;

/**
 * A "group" just comprising a single, non-bulkable request.
 */
public class SingleRequest implements BackendRequestGroup {

	private final JestClient jestClient;
	private final ErrorHandler errorHandler;
	private final BackendRequest<?> request;

	public SingleRequest(JestClient jestClient, ErrorHandler errorHandler, BackendRequest<?> request) {
		this.jestClient = jestClient;
		this.errorHandler = errorHandler;
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
		// DBQ ignores the refresh parameter for some reason, so doing it explicitly
		if ( request.getAction() instanceof DeleteByQuery ) {
			Refresh refresh = new Refresh.Builder()
				.addIndex( request.getIndexName() )
				.build();

			jestClient.executeRequest( refresh );
		}
	}

	@Override
	public int getSize() {
		return 1;
	}
}
