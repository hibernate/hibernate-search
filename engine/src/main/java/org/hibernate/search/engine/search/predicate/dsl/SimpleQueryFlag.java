/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

public enum SimpleQueryFlag {

	/**
	 * Enables {@code AND} operator (+)
	 */
	AND,
	/**
	 * Enables {@code NOT} operator (-)
	 */
	NOT,
	/**
	 * Enables {@code OR} operator (|)
	 */
	OR,
	/**
	 * Enables {@code PREFIX} operator (*)
	 */
	PREFIX,
	/**
	 * Enables {@code PHRASE} operator (")
	 */
	PHRASE,
	/**
	 * Enables {@code PRECEDENCE} operators: {@code (} and {@code )}
	 */
	PRECEDENCE,
	/**
	 * Enables {@code ESCAPE} operator (\)
	 */
	ESCAPE,
	/**
	 * Enables {@code WHITESPACE} operators: ' ' '\n' '\r' '\t'
	 */
	WHITESPACE,
	/**
	 * Enables {@code FUZZY} operators: (~) on single terms
	 */
	FUZZY,
	/**
	 * Enables {@code NEAR} operators: (~) on phrases
	 */
	NEAR
}
