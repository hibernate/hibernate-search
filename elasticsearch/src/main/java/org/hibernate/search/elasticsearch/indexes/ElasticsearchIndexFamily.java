/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.indexes;

import org.hibernate.search.indexes.IndexFamily;

public interface ElasticsearchIndexFamily extends IndexFamily {

	/**
	 * Retrieve the underlying, low-level client used to communicate with the Elasticsearch cluster.
	 * <p>
	 * <strong>WARNING - Unsupported API:</strong> the underlying client class may change without notice.
	 *
	 * @param clientClass The {@link Class} representing the expected client type
	 * @return The client.
	 * @throws org.hibernate.search.exception.SearchException if the underlying client does not implement the given class.
	 */
	<T> T getClient(Class<T> clientClass);

}
