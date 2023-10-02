/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * Whether index-time scoring information for the field should be stored or not.
 * <p>
 * Enabling norms will improve the quality of scoring.
 * Disabling norms will reduce the disk space used by the index.
 */
public enum Norms {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * The index-time scoring information is not stored.
	 */
	NO,
	/**
	 * The index-time scoring information is stored.
	 */
	YES
}
