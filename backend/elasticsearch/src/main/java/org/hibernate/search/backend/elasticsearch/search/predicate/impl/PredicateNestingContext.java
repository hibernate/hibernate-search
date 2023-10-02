/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.backend.elasticsearch.search.predicate.impl;

public class PredicateNestingContext {
	private static final PredicateNestingContext EMPTY = new PredicateNestingContext();
	private final String nestedPath;

	public static PredicateNestingContext simple() {
		return EMPTY;
	}

	public static PredicateNestingContext nested(String nestedPath) {
		return new PredicateNestingContext( nestedPath );
	}

	private PredicateNestingContext(String nestedPath) {
		this.nestedPath = nestedPath;
	}

	private PredicateNestingContext() {
		this( null );
	}

	public String getNestedPath() {
		return nestedPath;
	}
}
