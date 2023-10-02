/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.query;

/**
 * Defines the method used to initialize an object
 *
 * @author Emmanuel Bernard
 * @deprecated This setting is ignored in Hibernate Search 6.
 */
@Deprecated
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
