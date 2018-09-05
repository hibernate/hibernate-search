/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.exception.SearchException;

/**
 * An object responsible for dropping an indexes.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchSchemaDropper {

	/**
	 * Drops an index, throwing an exception if dropping fails.
	 *
	 * @param indexName The name of the index to drop.
	 * @param executionOptions The execution options, giving more context information.
	 * @throws SearchException If an error occurs.
	 */
	void drop(URLEncodedString indexName, ExecutionOptions executionOptions);

	/**
	 * Drops an index, throwing an exception if dropping fails.
	 *
	 * <p>This method will skip operations silently if the index does not exist.
	 *
	 * @param indexName The name of the index to drop.
	 * @param executionOptions The execution options, giving more context information.
	 * @throws SearchException If an error occurs.
	 */
	void dropIfExisting(URLEncodedString indexName, ExecutionOptions executionOptions);

}
