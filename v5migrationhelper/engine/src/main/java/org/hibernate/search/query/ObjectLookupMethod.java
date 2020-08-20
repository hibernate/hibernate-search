/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query;

/**
 * Define whether or not to check whether objects are already present in the second level cache or the persistence context.
 *
 * In most cases, no presence check is necessary.
 *
 * @author Emmanuel Bernard
 */
public enum ObjectLookupMethod {

	/**
	 * Skip checking (default)
	 */
	SKIP,

	/**
	 * Check whether an object is already in the persistence context before initializing it
	 */
	PERSISTENCE_CONTEXT,

	/**
	 * Check whether an object is already either in the persistence context or in the second level cache before loading it.
	 */
	SECOND_LEVEL_CACHE

}
