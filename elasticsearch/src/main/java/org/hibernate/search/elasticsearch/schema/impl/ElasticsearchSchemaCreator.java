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
 * An object responsible for creating an index based on provided metadata.
 *
 * @author Yoann Rodiere
 */
public interface ElasticsearchSchemaCreator extends Service {

	/**
	 * Create an index and populates mappings.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @throws SearchException If an error occurs.
	 */
	void create(IndexMetadata indexMetadata, ExecutionOptions executionOptions);

	/**
	 * Create an index and populates mappings, but only if the index doesn't already exist.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @throws SearchException If an error occurs.
	 */
	void createIfAbsent(IndexMetadata indexMetadata, ExecutionOptions executionOptions);

}
