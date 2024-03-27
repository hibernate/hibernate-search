/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;

/**
 * A search predicate builder, i.e. an object responsible for collecting parameters
 * and then building a search predicate.
 */
public interface SearchPredicateBuilder {

	void boost(float boost);

	void constantScore();

	/**
	 * @return An implementation-specific view of this builder,
	 * allowing the backend to call a {@code build()} method in particular.
	 */
	SearchPredicate build();

}
