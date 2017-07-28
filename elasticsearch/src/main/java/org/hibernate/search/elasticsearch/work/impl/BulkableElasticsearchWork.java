/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface BulkableElasticsearchWork<T> extends ElasticsearchWork<T> {

	JsonObject getBulkableActionMetadata();

	JsonObject getBulkableActionBody();

	/**
	 * @param context The execution context
	 * @param resultItem A future eventually returning the part of the bulk JSON result relevant to this work
	 * @return a future eventually returning the result of this work
	 */
	CompletableFuture<T> handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject resultItem);

}
