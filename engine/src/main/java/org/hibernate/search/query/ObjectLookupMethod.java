/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query;

/**
 * Define whether or not to check if objects are already present in:
 *  - the second level cache
 *  - the persistence context
 *
 * In most cases, no presence check is necessary.
 *
 * @author Emmanuel Bernard <emmanuel@hibernate.org>
 */
public enum ObjectLookupMethod {

	/**
	 * skip checking (default)
	 */
	SKIP,

	/**
	 * check whether an object is already in the persistence context
	 * before initializing it
	 */
	PERSISTENCE_CONTEXT,

	/**
	 * check whether an object is already either :
	 *  - in the persistence context
	 *  - in the second level cache
	 * before loading it.
	 */
	SECOND_LEVEL_CACHE

}
