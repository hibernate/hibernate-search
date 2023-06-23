/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.work.impl;

import org.hibernate.search.engine.backend.work.execution.DocumentRefreshStrategy;

import com.google.gson.JsonObject;

public interface BulkableWork<T> extends ElasticsearchWork {

	DocumentRefreshStrategy getRefreshStrategy();

	JsonObject getBulkableActionMetadata();

	JsonObject getBulkableActionBody();

	/**
	 * @param context The execution context
	 * @param resultItem A future eventually returning the part of the bulk JSON result relevant to this work
	 * @return the result of this work
	 */
	T handleBulkResult(ElasticsearchWorkExecutionContext context, JsonObject resultItem);

}
