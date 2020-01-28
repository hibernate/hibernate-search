/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.util.spi.URLEncodedString;
import org.hibernate.search.util.common.SearchException;

/**
 * An object responsible for creating an index and its mappings based on provided metadata.
 *
 */
public interface ElasticsearchSchemaCreator {

	/**
	 * Create an index and its mapping.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @param executionOptions The execution options, giving more context information.
	 * @return A future.
	 * @throws SearchException If an error occurs.
	 */
	CompletableFuture<?> createIndexAssumeNonExisting(IndexMetadata indexMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions);

	/**
	 * Create an index and its mapping, but only if the index doesn't already exist.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @param executionOptions The execution options, giving more context information.
	 * @return A future holding {@code true} if the index had to be created, {@code false} otherwise.
	 * @throws SearchException If an error occurs.
	 */
	CompletableFuture<Boolean> createIndexIfAbsent(IndexMetadata indexMetadata,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions);

	/**
	 * Checks that an index already exists.
	 *
	 * @param indexname The expected index name.
	 * @param executionOptions The execution options, giving more context information.
	 * @return A future.
	 * @throws SearchException If the index doesn't exist, or if an error occurs.
	 */
	CompletableFuture<?> checkIndexExists(URLEncodedString indexname,
			ElasticsearchIndexLifecycleExecutionOptions executionOptions);

}
