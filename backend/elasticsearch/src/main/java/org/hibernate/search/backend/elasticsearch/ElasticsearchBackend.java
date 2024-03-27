/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch;

import org.hibernate.search.engine.backend.Backend;
import org.hibernate.search.util.common.SearchException;

public interface ElasticsearchBackend extends Backend {

	/**
	 * Retrieve the underlying, low-level client used to communicate with the Elasticsearch cluster.
	 * <p>
	 * <strong>WARNING - Unsupported API:</strong> the underlying client class may change without notice.
	 *
	 * @param clientClass The {@link Class} representing the expected client type
	 * @param <T> The expected client type
	 * @return The client.
	 * @throws SearchException if the underlying client does not implement the given class.
	 */
	<T> T client(Class<T> clientClass);

}
