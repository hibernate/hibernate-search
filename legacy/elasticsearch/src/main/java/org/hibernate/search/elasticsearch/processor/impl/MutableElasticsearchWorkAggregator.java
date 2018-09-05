/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.processor.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.elasticsearch.work.impl.ElasticsearchWorkAggregator;

/**
 * @author Yoann Rodiere
 */
interface MutableElasticsearchWorkAggregator extends ElasticsearchWorkAggregator {

	/**
	 * Schedule any pending work for execution.
	 * <p>
	 * {@link #reset()} must be called afterwards if you want to reuse this aggregator.
	 *
	 * @return a future that will complete when all the pending work will be completed,
	 * canceled or or failed.
	 */
	CompletableFuture<Void> flush();

	/**
	 * Reset the state of this aggregator, removing any pending work.
	 */
	void reset();

}
