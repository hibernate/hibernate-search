/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.schema.management;

import java.nio.file.Path;

import org.hibernate.search.util.common.annotation.Incubating;

/**
 * The entry point for explicit schema management operations: creating indexes, dropping them, validating them, ...
 * <p>
 * A {@link SearchSchemaManager} targets a pre-defined set of indexed types (and their indexes).
 */
public interface SearchSchemaManager {

	/**
	 * Does not change indexes nor their schema,
	 * but checks that indexes exist and validates their schema.
	 * <p>
	 * An exception will be thrown if:
	 * <ul>
	 *     <li>Indexes are missing</li>
	 *     <li>OR, with Elasticsearch only, indexes exist but their schema does not match the requirements
	 *     of the Hibernate Search mapping:
	 *     missing fields, fields with incorrect type, missing analyzer definitions or normalizer definitions, ...
	 *     </li>
	 * </ul>
	 * <p>
	 * <strong>Warning:</strong> with the Lucene backend, validation is limited to checking that the indexes exist,
	 * because local Lucene indexes don't have a schema.
	 */
	void validate();

	/**
	 * Creates missing indexes and their schema,
	 * but does not touch existing indexes and assumes their schema is correct without validating it.
	 * <p>
	 * Note that creating indexes or updating their schema will not populate or update the indexed data:
	 * a newly created index will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 */
	void createIfMissing();

	/**
	 * Creates missing indexes and their schema,
	 * and validates the schema of existing indexes.
	 * <p>
	 * Note that creating indexes and their schema will not populate the indexed data:
	 * newly created indexes will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 * <p>
	 * With Elasticsearch only, an exception will be thrown on startup if some indexes already exist
	 * but their schema does not match the requirements of the Hibernate Search mapping:
	 * missing fields, fields with incorrect type, missing analyzer definitions or normalizer definitions, ...
	 * <p>
	 * <strong>Warning:</strong> with the Lucene backend, validation is limited to checking that the indexes exist,
	 * because local Lucene indexes don't have a schema.
	 */
	void createOrValidate();

	/**
	 * Creates missing indexes and their schema,
	 * and updates the schema of existing indexes if possible.
	 * <p>
	 * Note that creating indexes or updating their schema will not populate or update the indexed data:
	 * newly created indexes will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 * <p>
	 * <strong>Note:</strong> with the Lucene backend, schema update is a no-op,
	 * because local Lucene indexes don't have a schema.
	 * <p>
	 * <strong>Warning:</strong> with the Elasticsearch backend, if analyzer/normalizer definitions have to be updated,
	 * the index will be closed automatically during the update.
	 * <p>
	 * <strong>Warning:</strong> with the Elasticsearch backend, many scenarios can cause schema updates to fail:
	 * a field changed its type from string to integer, an analyzer definition changed, ...
	 * In such cases, the only workaround is to drop and re-create the index.
	 */
	void createOrUpdate();

	/**
	 * Drops existing indexes.
	 * <p>
	 * Note that dropping indexes means losing all indexed data.
	 */
	void dropIfExisting();

	/**
	 * Drops existing indexes and re-creates them and their schema.
	 * <p>
	 * Note that dropping indexes means losing all indexed data,
	 * and creating indexes will not populate them:
	 * the newly created index will always be empty.
	 * To populate indexes with pre-existing data, use mass indexing.
	 */
	void dropAndCreate();

	/**
	 * Exports the schema into a provided target directory. The output is backend specific.
	 * Generates a directory tree within the target directory based on the configured backends and indexed types.
	 *
	 * @param targetDirectory The directory to export to. Should be an empty directory.
	 * Exporting to a non-empty directory might result in an exception.
	 */
	@Incubating
	void exportSchema(Path targetDirectory);

}
