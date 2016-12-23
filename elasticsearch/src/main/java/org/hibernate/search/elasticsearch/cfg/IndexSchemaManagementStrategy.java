/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.elasticsearch.cfg;

/**
 * Strategy for creating/deleting the indexes in Elasticsearch upon session factory initialization and shut-down.
 *
 * @author Gunnar Morling
 */
public enum IndexSchemaManagementStrategy {

	/**
	 * Indexes will never be created or deleted. Hibernate Search will only check that the index actually exists.
	 * <p>The index schema (mapping and analyzer definitions) is not managed by Hibernate Search and is not checked.
	 */
	NONE,

	/**
	 * Upon session factory initialization, existing index mappings will be checked
	 * by Hibernate Search, causing an exception if a required mapping does not exist
	 * or exists but differs in a non-compatible way (more strict type constraints, for instance).
	 * <p>This strategy will not bring any change to the mappings or analyzer definitions, nor create or delete any index.
	 */
	VALIDATE,

	/**
	 * Upon session factory initialization, index mappings will be merged with existing ones, causing an exception if a
	 * mapping to be merged is not compatible with the existing one.
	 * <p>Missing indexes will be created along with their mappings. Missing mappings on existing indexes will be created.
	 */
	MERGE,

	/**
	 * Existing indexes will not be altered, missing indexes will be created along with their mappings and analyzer definitions.
	 */
	CREATE,

	/**
	 * Indexes - and all their contents - will be deleted and newly created (along with their mappings and analyzer definitions)
	 * upon session factory initialization.
	 *
	 * <p>Note that whenever a search factory is altered after initialization (i.e. new entities are mapped),
	 * the index will <strong>not</strong> be deleted again: new mappings will simply be added to the index.
	 */
	RECREATE,

	/**
	 * The same as {@link #RECREATE}, but indexes - and all their contents - will be deleted upon session factory
	 * shut-down as well.
	 */
	RECREATE_DELETE;
}
