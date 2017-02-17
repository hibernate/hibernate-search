/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import com.google.gson.JsonObject;

/**
 * @author Yoann Rodiere
 */
public interface BulkableElasticsearchWork<T> extends ElasticsearchWork<T> {

	JsonObject getBulkableActionMetadata();

	JsonObject getBulkableActionBody();

	/**
	 * @param context
	 * @param resultItem
	 * @return {@code true} if the result is considered a success, {@code false} otherwise.
	 */
	boolean handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject resultItem);

}
