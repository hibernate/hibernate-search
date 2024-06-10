/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial step in a "query string" predicate definition, where the target field can be set.
 *
 * @param <N> The type of the next step.
 */
public interface QueryStringPredicateFieldStep<N extends QueryStringPredicateFieldMoreStep<?, ?>>
		extends CommonQueryStringPredicateFieldStep<N> {

}
