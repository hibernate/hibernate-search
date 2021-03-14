/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.pojo.massindexing.loader;

/**
 * Options for mass indexing proccess.
 */
public interface MassIndexingOptions {

	/**
	 * @return the thread name prefix.
	 */
	String threadNamePrefix();

	/**
	 * @return the tenant identifier.
	 */
	String tenantIdentifier();

	/**
	 * @return the batch size used to load the root entities.
	 */
	int batchSize();

	/**
	 * @return the objects limit used to load the root entities.
	 */
	long objectsLimit();

	/**
	 * @return fetch size used to load the root entities.
	 */
	int fetchSize();

}
