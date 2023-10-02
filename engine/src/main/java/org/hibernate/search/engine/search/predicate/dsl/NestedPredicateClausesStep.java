/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.search.engine.search.predicate.dsl;

/**
 * The step in a nested predicate definition where
 * <a href="SimpleBooleanPredicateClausesCollector.html#clauses">clauses</a> can be added.
 * <p>
 * The resulting nested predicate must match <em>all</em> inner clauses,
 * similarly to an {@link SearchPredicateFactory#and() "and" predicate}.
 *
 * @param <S> The "self" type (the actual exposed type of this step).
 */
public interface NestedPredicateClausesStep<S extends NestedPredicateClausesStep<?>>
		extends GenericSimpleBooleanPredicateClausesStep<S, NestedPredicateClausesCollector<?>>,
		NestedPredicateClausesCollector<NestedPredicateClausesCollector<?>> {

	// TODO HSEARCH-3090 add tuning methods, like the "score_mode" in Elasticsearch (avg, min, ...)

}
