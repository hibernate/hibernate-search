/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.exception.SearchException;

import io.searchbox.action.Action;
import io.searchbox.client.JestResult;
import io.searchbox.core.BulkResult.BulkResultItem;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchRequestResultAssessor<T extends JestResult> {

	/**
	 * Checks the given detailed result, throwing an exception if the result is a failure.
	 * @param context The context in which the request was executed.
	 * @param request The request that produced the result.
	 * @param result The detailed result.
	 * @throws SearchException If the result is a failure.
	 */
	void checkSuccess(ElasticsearchWorkExecutionContext context, Action<? extends T> request, T result) throws SearchException;

	/**
	 * Checks the given summary result, return {@code true} if it is successful, {@code false} otherwise.
	 * @param context The context in which the request was executed.
	 * @param bulkResultItem The summary result.
	 * @return {@code true} if the result is successful, {@code false} otherwise.
	 */
	boolean isSuccess(ElasticsearchWorkExecutionContext context, BulkResultItem bulkResultItem);

}
