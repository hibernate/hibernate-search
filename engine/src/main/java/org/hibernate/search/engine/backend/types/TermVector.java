/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.backend.types;

/**
 * Defines the term vector storing strategy.
 */
public enum TermVector {
	/**
	 * Use the backend-specific default.
	 */
	DEFAULT,
	/**
	 * Store term vectors.
	 */
	YES,
	/**
	 * Do not store term vectors.
	 */
	NO,
	/**
	 * Store the term vectors. Also store token positions into the term.
	 */
	WITH_POSITIONS,
	/**
	 * Store the term vectors. Also store token character offsets into the term.
	 */
	WITH_OFFSETS,
	/**
	 * Store the term vectors. Also store token positions and token character offsets into the term.
	 */
	WITH_POSITIONS_OFFSETS,
	/**
	 * Store the term vectors. Also store token positions and token payloads into the term.
	 */
	WITH_POSITIONS_PAYLOADS,
	/**
	 * Store the term vectors. Also store token positions, token character offsets and token payloads into the term.
	 */
	WITH_POSITIONS_OFFSETS_PAYLOADS,
}
