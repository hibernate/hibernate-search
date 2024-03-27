/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.spi;

import org.hibernate.search.engine.search.predicate.SearchPredicate;
import org.hibernate.search.util.common.annotation.Incubating;

@Incubating
public interface KnnPredicateBuilder extends SearchPredicateBuilder {

	void k(int k);

	void vector(Object vector);

	void filter(SearchPredicate filter);

	void requiredMinimumSimilarity(float similarity);

	/**
	 * @return An implementation-specific view of this builder,
	 * allowing the backend to call a {@code build()} method in particular.
	 */
	SearchPredicate build();
}
