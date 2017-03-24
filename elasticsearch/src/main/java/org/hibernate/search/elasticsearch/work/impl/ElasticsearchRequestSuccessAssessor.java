/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.elasticsearch.client.Response;
import org.hibernate.search.elasticsearch.client.impl.ElasticsearchRequest;
import org.hibernate.search.exception.SearchException;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchRequestSuccessAssessor {

	/**
	 * Check the given response, throwing an exception if the reponse indicates a failure.
	 * @param context The context in which the request was executed.
	 * @param request The request whose success is to be assessed.
	 * @param response The response, containing information about the outcome of the request.
	 * @param parsedResponseBody The response body parsed as JSON.
	 * @throws SearchException If the result is a failure.
	 */
	void checkSuccess(ElasticsearchWorkExecutionContext context, ElasticsearchRequest request, Response response,
			JsonObject parsedResponseBody) throws SearchException;

	/**
	 * Check the given response, return {@code true} if it is successful, {@code false} otherwise.
	 * @param context The context in which the request was executed.
	 * @param bulkResponseItem The part of the response body concerning the request whose success is to be assessed.
	 * @return {@code true} if the result is successful, {@code false} otherwise.
	 */
	boolean isSuccess(ElasticsearchWorkExecutionContext context, JsonObject bulkResponseItem);

}
