/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.elasticsearch.cfg;

/**
 * Strategy for creating/deleting the indexes in Elasticsearch upon session factory initialization and shut-down.
 *
 * @author Gunnar Morling
 */
public enum IndexManagementStrategy {

	/**
	 * Indexes will never be created or deleted.
	 */
	NONE,

	/**
	 * Upon session factory initialization, index mappings will be merged with existing ones, causing an exception if a
	 * mapping to be merged is not compatible with the existing one. Missing indexes and mappings will be created.
	 */
	MERGE,

	/**
	 * Indexes - and all their contents - will be deleted and newly created upon session factory initialization.
	 */
	CREATE,

	/**
	 * The same as {@link #CREATE}, but indexes - and all their contents - will be deleted upon session factory
	 * shut-down.
	 */
	CREATE_DELETE;
}
