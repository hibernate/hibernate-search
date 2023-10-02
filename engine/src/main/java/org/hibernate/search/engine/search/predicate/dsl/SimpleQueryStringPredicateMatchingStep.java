/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a "simple query string" predicate definition where the query string to match can be set.
 *
 * @param <N> The type of the next step.
 */
public interface SimpleQueryStringPredicateMatchingStep<N extends SimpleQueryStringPredicateOptionsStep<?>>
		extends CommonQueryStringPredicateMatchingStep<N> {

}
