/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "query string" predicate definition where the query string to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface QueryStringPredicateMatchingStep<N extends QueryStringPredicateOptionsStep<?>>
		extends CommonQueryStringPredicateMatchingStep<N> {

}
