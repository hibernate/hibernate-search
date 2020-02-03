/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import java.util.concurrent.CompletableFuture;

import org.hibernate.search.backend.elasticsearch.index.naming.impl.IndexNames;
import org.hibernate.search.backend.elasticsearch.work.result.impl.ExistingIndexMetadata;
import org.hibernate.search.backend.elasticsearch.lowlevel.index.impl.IndexMetadata;
import org.hibernate.search.util.common.SearchException;

/**
 * An object responsible for creating an index and its mappings based on provided metadata.
 *
 */
public interface ElasticsearchSchemaCreator {

	/**
	 * Create an index and its mapping.
	 *
	 * @param indexNames The index names.
	 * @param indexMetadata The expected index metadata.
	 * @return A future.
	 * @throws SearchException If an error occurs.
	 */
	CompletableFuture<?> createIndexAssumeNonExisting(IndexNames indexNames, IndexMetadata indexMetadata);

	/**
	 * Create an index and its mapping, but only if the index doesn't already exist.
	 *
	 * @param indexNames The index names.
	 * @param indexMetadata The expected index metadata.
	 * @return A future holding the metadata of the pre-existing index, or null if the index had to be created.
	 * @throws SearchException If an error occurs.
	 */
	CompletableFuture<ExistingIndexMetadata> createIndexIfAbsent(IndexNames indexNames, IndexMetadata indexMetadata);

}
