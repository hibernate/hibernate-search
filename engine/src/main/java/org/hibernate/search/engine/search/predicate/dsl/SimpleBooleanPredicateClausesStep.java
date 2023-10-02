/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The initial and final step in a "simple boolean predicate" definition,
 * where <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a>
 * can be added and options can be set.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface SimpleBooleanPredicateClausesStep<S extends SimpleBooleanPredicateClausesStep<?>>
		extends GenericSimpleBooleanPredicateClausesStep<S, SimpleBooleanPredicateClausesCollector<?>>,
		SimpleBooleanPredicateOptionsStep<S> {

}
