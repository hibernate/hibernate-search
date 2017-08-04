/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.work.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.LuceneWork;

/**
 * A unit of work to be delegated to the Elasticsearch backend.
 * <p>
 * Classes implementing this interface know about the data to be sent (the request),
 * but also about how to handle the response (callbacks, exception throwing).
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchWork<T> {

	CompletableFuture<T> execute(ElasticsearchWorkExecutionContext executionContext);

	void aggregate(ElasticsearchWorkAggregator aggregator);

	/**
	 * @return the original Lucene works from which this work was derived.
	 */
	LuceneWork getLuceneWork();

}
