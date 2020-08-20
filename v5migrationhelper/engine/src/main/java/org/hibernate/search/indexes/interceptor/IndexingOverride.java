/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.indexes.interceptor;

/**
 * Set of possible indexing operations
 *
 * @author Emmanuel Bernard
 */
public enum IndexingOverride {
	/**
	 * Let Hibernate Search engine apply the standard operation
	 * without overriding it
	 */
	APPLY_DEFAULT,

	/**
	 * Skip any indexing operation
	 */
	SKIP,

	/**
	 * Force an entity to be removed from the index
	 * This operation can be safely requested regardless
	 * of the actual presence of the entity in the index
	 */
	REMOVE,

	/**
	 * Update the entity index.
	 *
	 * It is safe to update an entity that has not been added yet.
	 */
	UPDATE
}
