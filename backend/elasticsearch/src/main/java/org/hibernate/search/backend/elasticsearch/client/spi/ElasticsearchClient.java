/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.client.spi;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.util.common.SearchException;

/**
 * An Elasticsearch client, allowing to perform requests to a remote cluster.
 *
 */
public interface ElasticsearchClient {

	/**
	 * @param request A request to execute asynchronously
	 * @return The future that will ultimately hold the response
	 * (or throw an exception if an error occurred or if the request timed out).
	 */
	CompletableFuture<ElasticsearchResponse> submit(ElasticsearchRequest request);

	/**
	 * Unwrap the client to some implementation-specific type.
	 *
	 * @param clientClass The {@link Class} representing the expected client type
	 * @param <T> The expected client type
	 * @return The unwrapped client.
	 * @throws SearchException if the client implementation does not support
	 * unwrapping to the given class.
	 */
	<T> T unwrap(Class<T> clientClass);

}
