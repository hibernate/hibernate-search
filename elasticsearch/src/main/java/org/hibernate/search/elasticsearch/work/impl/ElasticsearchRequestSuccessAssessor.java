/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import org.hibernate.search.elasticsearch.client.impl.ElasticsearchResponse;
import org.hibernate.search.exception.SearchException;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface ElasticsearchRequestSuccessAssessor {

	/**
	 * Check the given response, throwing an exception if the reponse indicates a failure.
	 * @param response The response, containing information about the outcome of the request.
	 * @throws SearchException If the result is a failure.
	 */
	void checkSuccess(ElasticsearchResponse response) throws SearchException;

	/**
	 * Check the given bulk response item, return {@code true} if it is successful, {@code false} otherwise.
	 * @param bulkResponseItem The part of the response body concerning the request whose success is to be assessed.
	 * @throws SearchException If the result is a failure.
	 */
	void checkSuccess(JsonObject bulkResponseItem);

}
