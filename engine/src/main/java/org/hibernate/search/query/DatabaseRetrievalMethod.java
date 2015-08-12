/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.query;

/**
 * Defines the method used to initialize an object
 *
 * @author Emmanuel Bernard
 */
public enum DatabaseRetrievalMethod {
	/**
	 * Use a criteria query to load the objects.
	 * This is done in batch to minimize the number of queries
	 *
	 * Default approach
	 */
	QUERY,

	/**
	 * Load each object by its identifier one by one.
	 * Useful if a batch size is set in the entity's mapping
	 */
	FIND_BY_ID
}
