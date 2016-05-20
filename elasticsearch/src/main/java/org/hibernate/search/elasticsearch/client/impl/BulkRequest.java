/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.hibernate.search.backend.LuceneWork;
import org.hibernate.search.exception.ErrorHandler;
import org.hibernate.search.exception.impl.ErrorContextBuilder;

/**
 * A request group backed by an actual bulk request.
 */
public class BulkRequest implements ExecutableRequest {

	private final JestClient jestClient;
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

	public BulkRequest(JestClient jestClient, ErrorHandler errorHandler, List<BackendRequest<?>> requests, Set<String> indexNames, Set<String> indexesNeedingRefresh, boolean refresh) {
		this.jestClient = jestClient;
		this.errorHandler = errorHandler;
		this.requests = requests;
		this.indexNames = indexNames;
		this.indexesNeedingRefresh = indexesNeedingRefresh;
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
}
