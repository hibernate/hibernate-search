/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether the field can be used in projections.
 * <p>
 * This usually means that the field will have doc-values stored in the index.
 */
public enum Aggregable {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * Do not allow aggregation on the field.
	 */
	NO,
	/**
	 * Allow aggregation on the field.
	 */
	YES
}
