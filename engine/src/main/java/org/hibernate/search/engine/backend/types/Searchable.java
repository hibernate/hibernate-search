/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether or not we want to be able to search the document using this field.
 */
public enum Searchable {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * The field is not searchable, i.e. search predicates cannot be applied to the field.
	 * <p>
	 * This can save some disk space if you know the field is only used for projections or sorts.
	 */
	NO,
	/**
	 * The field is searchable, i.e. search predicates can be applied to the field.
	 */
	YES
}
