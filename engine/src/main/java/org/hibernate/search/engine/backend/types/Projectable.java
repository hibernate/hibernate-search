/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether or not we want to be able to obtain the value of the field as a projection.
 * <p>
 * This usually means that the field will be stored in the index but it is more subtle than that, for instance in the
 * case of projection by distance.
 */
public enum Projectable {
	/**
	 * Use the backend-specific default.
	 * <ul>
	 *     <li>Lucene's default value is {@code YES} </li>
	 *     <li>Elasticsearch's default value is {@code NO} </li>
	 * </ul>
	 */
	DEFAULT,
	/**
	 * Do not allow projection on the field.
	 */
	NO,
	/**
	 * Allow projection on the field.
	 */
	YES
}
