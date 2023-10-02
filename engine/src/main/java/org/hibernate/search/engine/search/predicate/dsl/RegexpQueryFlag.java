/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

public enum RegexpQueryFlag {

	/**
	 * Enables {@code INTERVAL} operator ({@code <>})
	 */
	INTERVAL,
	/**
	 * Enables {@code INTERSECTION} operator ({@code &})
	 */
	INTERSECTION,
	/**
	 * Enables {@code ANY_STRING} operator ({@code @})
	 */
	ANY_STRING

}
