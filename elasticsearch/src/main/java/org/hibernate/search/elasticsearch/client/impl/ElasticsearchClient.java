/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.client.impl;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * An Elasticsearch client, allowing to perform requests to a remote cluster.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchClient extends Closeable {

	/**
	 * @param request A request to execute asynchronously
	 * @return The future that will ultimately hold the response
	 * (or throw an exception if an error occurred or if the request timed out).
	 */
	CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request);

}
