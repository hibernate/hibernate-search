/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

import org.hibernate.search.engine.search.reference.predicate.SimpleQueryStringPredicateFieldReference;

/**
 * The initial step in a "simple query string" predicate definition, where the target field can be set.
 *
 * @param <SR> Scope root type.
 * @param <N> The type of the next step.
 */
public interface SimpleQueryStringPredicateFieldStep<SR, N extends SimpleQueryStringPredicateFieldMoreStep<SR, ?, ?>>
		extends CommonQueryStringPredicateFieldStep<SR, N, SimpleQueryStringPredicateFieldReference<SR, ?>> {

}
