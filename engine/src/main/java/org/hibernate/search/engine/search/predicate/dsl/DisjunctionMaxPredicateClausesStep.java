/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial and final step in a "disjunction max" predicate definition, where
 * <a href="DisjunctionMaxPredicateClausesCollector.html#disjuncts">disjuncts</a>
 * can be added and options can be set.
 *
 * @param <SR> Scope root type.
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface DisjunctionMaxPredicateClausesStep<SR, S extends DisjunctionMaxPredicateClausesStep<SR, ?>>
		extends DisjunctionMaxPredicateClausesCollector<SR, S>, DisjunctionMaxPredicateOptionsStep<S> {

}
