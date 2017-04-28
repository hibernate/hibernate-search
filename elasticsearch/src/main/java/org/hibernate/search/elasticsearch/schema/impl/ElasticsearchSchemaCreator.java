/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.client.impl.URLEncodedString;
import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.exception.SearchException;

/**
 * An object responsible for creating an index and its mappings based on provided metadata.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchSchemaCreator {

	/**
	 * Create an index.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @param executionOptions The execution options, giving more context information.
	 * @throws SearchException If an error occurs.
	 */
	void createIndex(IndexMetadata indexMetadata, ExecutionOptions executionOptions);

	/**
	 * Create an index, but only if the index doesn't already exist.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @param executionOptions The execution options, giving more context information.
	 * @return {@code true} if the index had to be created, {@code false} otherwise.
	 * @throws SearchException If an error occurs.
	 */
	boolean createIndexIfAbsent(IndexMetadata indexMetadata, ExecutionOptions executionOptions);

	/**
	 * Checks that an index already exists.
	 *
	 * @param indexname The expected index name.
	 * @param executionOptions The execution options, giving more context information.
	 * @throws SearchException If the index doesn't exist, or if an error occurs.
	 */
	void checkIndexExists(URLEncodedString indexname, ExecutionOptions executionOptions);

	/**
	 * Create mappings on a supposedly existing index.
	 *
	 * <p>Mappings are supposed to be absent from the index.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @param executionOptions The execution options, giving more context information.
	 * @throws SearchException If an error occurs.
	 */
	void createMappings(IndexMetadata indexMetadata, ExecutionOptions executionOptions);

}
