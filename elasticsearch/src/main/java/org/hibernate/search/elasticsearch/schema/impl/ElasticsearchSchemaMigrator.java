/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.schema.impl;

import org.hibernate.search.elasticsearch.schema.impl.model.IndexMetadata;
import org.hibernate.search.engine.service.spi.Service;
import org.hibernate.search.exception.SearchException;

/**
 * An object responsible for merging an existing index with provided metadata.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchSchemaMigrator extends Service {

	/**
	 * Merge metadata with the existing schema:
	 * <ul>
	 * <li>Create the index if it doesn't exist
	 * <li>For each mapping, merge the existing mapping with the expected one, throwing {@link SearchException}
	 * if an incompatible attribute is detected.
	 * </ul>
	 *
	 * @param indexMetadata The expected index metadata.
	 * @throws SearchException If an error occurs.
	 */
	void merge(IndexMetadata indexMetadata, ExecutionOptions executionOptions);

}
