/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.spi;

/**
 * Enumeration of the different types of Lucene work. This enumeration is used to specify the type
 * of index operation to be executed.
 *
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 * @author John Griffin
 */
public enum WorkType {
	ADD,

	UPDATE,

	DELETE,

	COLLECTION,

	/**
	 * Used to remove a specific instance of a class from an index.
	 */
	PURGE,

	/**
	 * Used to remove all instances of a class from an index.
	 */
	PURGE_ALL,

	/**
	 * This type is used for batch indexing.
	 */
	INDEX,

	DELETE_BY_QUERY
}
