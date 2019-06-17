/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.index.admin.impl;

import org.hibernate.search.util.common.SearchException;

/**
 * An object responsible for updating an existing index to match provided metadata.
 *
 */
public interface ElasticsearchSchemaMigrator {

	/**
	 * Update the existing schema to match the given metadata: for each mapping,
	 * update the existing mappings and analyzer definitions to match the expected ones,
	 * throwing {@link SearchException} if an incompatible attribute is detected.
	 *
	 * <p>The index is expected to already exist.
	 *
	 * @param indexMetadata The expected index metadata.
	 * @throws SearchException If an error occurs.
	 */
	void migrate(IndexMetadata indexMetadata);

}
