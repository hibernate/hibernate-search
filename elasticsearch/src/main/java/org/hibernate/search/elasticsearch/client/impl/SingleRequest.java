/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.util.Collections;
import java.util.Set;

import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * A single, non-bulkable request.
 *
 * @author Gunnar Morling
 */
public class SingleRequest implements ExecutableRequest {

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
	public Set<String> getTouchedIndexes() {
		return Collections.singleton( request.getIndexName() );
	}

	@Override
	public Set<String> getIndexesNeedingRefresh() {
		return request.needsRefreshAfterWrite() ? Collections.singleton( request.getIndexName() ) : Collections.<String>emptySet();
	}

	@Override
	public int getSize() {
		return 1;
	}
}
