/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a query string predicate definition, where the query string to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface CommonQueryStringPredicateMatchingStep<N extends CommonQueryStringPredicateOptionsStep<?>> {

	/**
	 * Require at least one of the targeted fields to match the given query string.
	 *
	 * @param queryString The query string to match.
	 * @return The next step.
	 */
	N matching(String queryString);

}
